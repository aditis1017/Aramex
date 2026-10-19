package com.customcharge.dashboard.importfile.service;

import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.util.aws.S3Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ImportTemplateDownloadService {

    private static final Logger log = LoggerFactory.getLogger(ImportTemplateDownloadService.class);

    private final ResourceLoader resourceLoader;
    private final ObjectProvider<S3Helper> s3HelperProvider;
    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;
    private final ImportColumnHeaderSupport importColumnHeaderSupport;

    public ImportTemplateDownloadService(
            ResourceLoader resourceLoader,
            ObjectProvider<S3Helper> s3HelperProvider,
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
            ImportColumnHeaderSupport importColumnHeaderSupport) {
        this.resourceLoader = resourceLoader;
        this.s3HelperProvider = s3HelperProvider;
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
        this.importColumnHeaderSupport = importColumnHeaderSupport;
    }

    public TemplateStream open(String templateKey, String entity) {
        String fileName = templateKey.strip();
        List<String> chargeList = new ArrayList<>();
        Optional<CategoryValue> categoryValue = categoryCodesGlobalDataCacheService.findCategoryByCode(fileName);
        if (categoryValue.isPresent()) {
            Optional<CategoryValue> tempCharges =
                    categoryCodesGlobalDataCacheService.findCategoryByCode(templateKey + ImportConstants.CHARGE_SUFFIX);
            if (tempCharges.isPresent()) {
                chargeList = ImportColumnHeaderSupport.parseCommaSeparated(tempCharges.get().getLabel());
            }
            fileName = categoryValue.get().getName();
        }
        TemplateStream s3Template = openFromS3(fileName, entity, chargeList);
        if (s3Template != null) {
            return withDownloadFileName(s3Template, entity);
        }

        return withDownloadFileName(openFromClasspath(fileName), entity);
    }

    private static TemplateStream withDownloadFileName(TemplateStream stream, String entity) {
        return new TemplateStream(toDownloadFileName(stream.fileName(), entity), stream.body());
    }

    private static String toDownloadFileName(String fileName, String entity) {
        if (fileName == null || fileName.isBlank() || entity == null || entity.isBlank()) {
            return fileName;
        }
        String entityCode = cleanPathSegment(entity);
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return fileName + "-" + entityCode;
        }
        String baseName = fileName.substring(0, extensionIndex);
        String extension = fileName.substring(extensionIndex);
        return baseName + "-" + entityCode + extension;
    }

    private TemplateStream openFromS3(String fileName, String entity, List<String> chargeList) {
        S3Helper s3Helper = s3HelperProvider.getIfAvailable();
        if (s3Helper == null) {
            return null;
        }

        String s3Key = s3Key(fileName, entity);
        try (InputStream ignored = s3Helper.getAsStream(s3Key).orElse(null)) {
            if (ignored == null) {
                return null;
            }
        } catch (Exception ex) {
            log.warn("Unable to check S3 template key {}. Falling back to classpath template.", s3Key, ex);
            return null;
        }

        List<String> extraHeaders = importColumnHeaderSupport.resolveServiceDescriptionHeaders(entity, chargeList);
        boolean enrichHeaders = !extraHeaders.isEmpty();

        StreamingResponseBody body = output -> {
            try (InputStream in = s3Helper.getAsStream(s3Key)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ImportConstants.MESSAGE_TEMPLATE_FILE_NOT_FOUND, fileName)))) {
                if (enrichHeaders) {
                    ImportTemplateXlsxHeaderEnricher.writeTemplateWithExtraHeaders(in, output, extraHeaders);
                } else {
                    in.transferTo(output);
                }
                output.flush();
            }
        };
        return new TemplateStream(fileName, body);
    }

    private TemplateStream openFromClasspath(String fileName) {
        Resource resource = resourceLoader.getResource(ImportConstants.CLASSPATH_TEMPLATE_PREFIX + fileName);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException(
                    String.format(ImportConstants.MESSAGE_TEMPLATE_FILE_NOT_FOUND, fileName));
        }
        StreamingResponseBody body = output -> {
            try (InputStream in = resource.getInputStream()) {
                in.transferTo(output);
                output.flush();
            }
        };
        return new TemplateStream(fileName, body);
    }

    private static String s3Key(String fileName, String entity) {
        String folder = entity == null || entity.isBlank() ? ImportConstants.S3_GLOBAL_FOLDER : cleanPathSegment(entity);
        return ImportConstants.S3_TEMPLATE_PREFIX + "/" + folder + "/" + fileName;
    }

    private static String cleanPathSegment(String raw) {
        String cleaned = raw.strip().replace('\\', '/');
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        return ImportConstants.PATH_SEGMENT_SANITIZE_PATTERN.matcher(cleaned).replaceAll("_");
    }

    public record TemplateStream(String fileName, StreamingResponseBody body) {
    }
}
