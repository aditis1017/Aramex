<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            font-family: Arial, sans-serif;
            font-size: 14px;
            color: #333333;
        }
        .container {
            width: 100%;
            padding: 12px;
        }
        .title {
            font-size: 18px;
            font-weight: bold;
            margin-bottom: 12px;
        }
        .details-table {
            border-collapse: collapse;
            width: 100%;
            margin-top: 10px;
        }
        .details-table td {
            border: 1px solid #dddddd;
            padding: 8px;
        }
        .label {
            font-weight: bold;
            background-color: #f5f5f5;
            width: 30%;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="title">Custom Charge Error Report</div>

    <p>Please find the generated error report details below.</p>

    <table class="details-table">
        <tr>
            <td class="label">File ID</td>
            <td>${fileId}</td>
        </tr>
        <tr>
            <td class="label">File Name</td>
            <td>${fileName}</td>
        </tr>
        <tr>
            <td class="label">Generated At</td>
            <td>${generatedAt}</td>
        </tr>
        <tr>
            <td class="label">Download Link</td>
            <td>
                    <#if downloadUrl?? && downloadUrl?has_content>
                        <a href="${downloadUrl?html}">Download Error Report</a>
                    <#else>
                        Download link is not available. Please contact support with File ID: ${fileId}
                    </#if>
                </td>
        </tr>
    </table>
</div>
</body>
</html>
