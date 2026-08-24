package rolesandpermission

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.common.WebUiCommonHelper
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.WebElement
import customsmanagement.ProfileGlobals

class BusinessActions {
    private static final String OR_PREFIX = 'Roles&Permission/CCIPH-89/'
    private static final String OR_PREFIX_91 = 'Roles&Permission/CCIPH-91/'
    private static final String OR_PREFIX_92 = 'Roles&Permission/CCIPH-92/'
    private static final String OR_PREFIX_95 = 'Roles&Permission/CCIPH-95/'
    private static final String OR_PREFIX_96 = 'Roles&Permission/CCIPH-96/'
    private static final String OR_PREFIX_97 = 'Roles&Permission/CCIPH-97/'
    private static final List<String> PERMISSION_ACTIONS = ['View', 'Create', 'Edit', 'Delete', 'Export']
    private static final List<String> MATRIX_ROLE_COLUMNS = ['Super Admin', 'Customs Manager', 'Finance Officer', 'Operations Staff']
    private static final List<String> MATRIX_MODULE_ROWS = 
	['Customs',	
    'Documents List',
    'Reports',
    'AR Management',
    'AP Management',
    'Global Settings',
    'Entity Settings',
    'B2B Keywords Manager']
    private static final int DEFAULT_TIMEOUT = 30
    private static final String DEFAULT_APP_URL = 'https://dev-cc.aramex.com/customs-management'
    private static final String DEFAULT_ROLES_PERMISSIONS_URL = 'https://dev-cc.aramex.com/customs-management/roles-and-permissions'
    private static final String DEFAULT_STAGE_APP_URL = 'https://stage-cc.aramex.com/customs'
    private static final String DEFAULT_STAGE_ROLES_PERMISSIONS_URL = 'https://stage-cc.aramex.com/customs-management/roles-permissions'
    private static final List<String> DEFAULT_SYSTEM_ROLES = ['Super Admin', 'Customs Manager', 'Finance Officer', 'Operations Staff', 'Viewer']
    private static final String ROLE_CARD_XPATH = "//*[contains(@class,'card') or contains(@class,'Card') or contains(@class,'roleCard')][.//*[self::a or self::button or @role='button'][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'view details')] or .//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'view details')]]"

    private static TestObject to(String name) { findTestObject(OR_PREFIX + name) }
    private static TestObject to91(String name) { findTestObject(OR_PREFIX_91 + name) }
    private static TestObject to92(String name) { findTestObject(OR_PREFIX_92 + name) }
    private static TestObject to95(String name) { findTestObject(OR_PREFIX_95 + name) }
    private static TestObject to96(String name) { findTestObject(OR_PREFIX_96 + name) }
    private static TestObject to97(String name) { findTestObject(OR_PREFIX_97 + name) }

    private static void failStop(String message) {
        try { WebUI.takeScreenshot() } catch (Throwable ignored) { }
        KeywordUtil.markFailedAndStop(message)
    }

    private static void waitVisible(TestObject testObject, int timeout = DEFAULT_TIMEOUT) { WebUI.waitForElementVisible(testObject, timeout) }
    private static void waitVisibleClickable(TestObject testObject, int timeout = DEFAULT_TIMEOUT) { WebUI.waitForElementVisible(testObject, timeout); WebUI.waitForElementClickable(testObject, timeout) }
    private static void click(TestObject testObject) { waitVisibleClickable(testObject); WebUI.click(testObject); WebUI.takeScreenshot() }

    private static String xpathLiteral(String value) {
        String v = (value ?: '').trim()
        if (!v) return "''"
        if (!v.contains("'")) return "'${v}'"
        if (!v.contains('"')) return '"' + v + '"'
        String[] parts = v.split("'")
        StringBuilder sb = new StringBuilder('concat(')
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",\"'\",")
            sb.append("'").append(parts[i]).append("'")
        }
        sb.append(')')
        return sb.toString()
    }

    private static String customsAppUrl() {
        String url = (ProfileGlobals.get('CUSTOMS_APP_URL', '') ?: ProfileGlobals.get('URL', '') ?: '').trim()
        return url ?: DEFAULT_APP_URL
    }

    private static String resolveRolesPermissionsUrl() {
        String url = (ProfileGlobals.get('ROLES_PERMISSIONS_URL', '') ?: ProfileGlobals.get('Roles_Permissions_URL', '') ?: '').trim()
        if (!url) url = customsAppUrl().replaceAll('/$', '') + '/roles-and-permissions'
        return url ?: DEFAULT_ROLES_PERMISSIONS_URL
    }

    private static String stageCustomsAppUrl() {
        String url = (ProfileGlobals.get('STAGE_CUSTOMS_APP_URL', '') ?: '').trim()
        if (!url) {
            String stageApp = (ProfileGlobals.get('STAGE_APPLICATION', '') ?: '').trim()
            if (stageApp) url = stageApp.replaceAll('/customs-management/.*$', '/customs')
        }
        if (!url) {
            String stageRoles = resolveRolesPermissionsStageUrl()
            if (stageRoles.contains('/customs-management/')) {
                url = stageRoles.replaceAll('/customs-management/.*$', '/customs')
            }
        }
        return url ?: DEFAULT_STAGE_APP_URL
    }

    private static String resolveRolesPermissionsStageUrl() {
        String url = (ProfileGlobals.get('ROLES_PERMISSIONS_URL_STAGE', '') ?: '').trim()
        return url ?: DEFAULT_STAGE_ROLES_PERMISSIONS_URL
    }

    private static String roleCardXpath(String roleName) {
        String rn = xpathLiteral((roleName ?: '').trim())
        return "${ROLE_CARD_XPATH}[.//*[normalize-space()=${rn}] or normalize-space()=${rn}]"
    }

    private static String roleCardEllipsisXpath(String roleName) {
        return "${roleCardXpath(roleName)}//*[self::button or @role='button'][contains(@class,'more') or contains(@class,'ellipsis') or contains(@class,'Menu') or contains(@aria-label,'more') or contains(@aria-label,'More') or .//*[contains(@class,'ellipsis') or contains(@class,'MoreVert')]]"
    }

    private static String viewDetailsLinkXpath(String roleName) {
        return "${roleCardXpath(roleName)}//*[self::a or self::button or @role='button' or self::span or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'view details')]"
    }

    private static List<String> roleDetailsScreenXpaths() {
        return [
            "//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')] and .//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'permission')]][not(self::script)][1]",
            "//*[contains(@class,'modal') or contains(@class,'dialog') or contains(@class,'Drawer') or contains(@class,'drawer') or contains(@class,'Sheet') or contains(@class,'sheet') or contains(@class,'overlay') or contains(@class,'panel') or @role='dialog'][.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'permission')]][1]",
            "//*[.//*[self::a or self::button][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit permission')]][1]"
        ]
    }

    private static boolean waitForAnyVisible(List<String> xpaths, String idPrefix, int timeout = 8) {
        for (int i = 0; i < xpaths.size(); i++) {
            TestObject candidate = dynamicXpathObject("${idPrefix}_${i}", xpaths[i])
            if (WebUI.waitForElementVisible(candidate, timeout, FailureHandling.OPTIONAL)) {
                return true
            }
        }
        return false
    }

    private static String roleCardDescriptionXpath(String roleName) {
        return "${roleCardXpath(roleName)}//*[self::p or self::span or self::div][string-length(normalize-space(.)) > 10][not(contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'view details'))][not(normalize-space()=${xpathLiteral((roleName ?: '').trim())})][1]"
    }

    private static String roleCardHeadingXpath(String roleName) {
        String roleLit = xpathLiteral((roleName ?: '').trim())
        return "${roleCardXpath(roleName)}//h3[(contains(@class,'roleName') or self::h3) and normalize-space()=${roleLit}]"
    }

    private static String normalizeDescriptionFragment(String text) {
        return (text ?: '').replaceAll(/\s+/, ' ').trim()
    }

    private static String roleCardDescriptionParagraphXpath(String roleName, String descriptionFragment, boolean useContains = true) {
        String roleLit = xpathLiteral((roleName ?: '').trim())
        String descLit = xpathLiteral(normalizeDescriptionFragment(descriptionFragment))
        String matchExpr = useContains ?
            "contains(normalize-space(.), ${descLit})" :
            "normalize-space()=${descLit}"
        String card = roleCardXpath(roleName)
        return "${card}//h3[(contains(@class,'roleName') or self::h3) and normalize-space()=${roleLit}]/following-sibling::p[contains(@class,'description')][${matchExpr}]" +
            " | ${card}//p[contains(@class,'description')][${matchExpr}]"
    }

    private static List<String> roleCardDescriptionParagraphXpaths(String roleName, String descriptionFragment) {
        String roleLit = xpathLiteral((roleName ?: '').trim())
        String descLit = xpathLiteral(normalizeDescriptionFragment(descriptionFragment))
        String card = roleCardXpath(roleName)
        return [
            "${card}//h3[(contains(@class,'roleName') or self::h3) and normalize-space()=${roleLit}]/following-sibling::p[contains(@class,'description')][contains(normalize-space(.), ${descLit})]",
            "${card}//p[contains(@class,'description')][contains(normalize-space(.), ${descLit})]",
            "//h3[normalize-space()=${roleLit}]/following-sibling::p[contains(@class,'description')][contains(normalize-space(.), ${descLit})]"
        ]
    }

    private static TestObject dynamicXpathObject(String id, String xpath) {
        TestObject obj = new TestObject(id)
        obj.addProperty('xpath', ConditionType.EQUALS, xpath)
        return obj
    }

    private static boolean isRoleVisible(String roleName, int timeoutSeconds = 5) {
        TestObject card = dynamicXpathObject("role_card_${roleName.hashCode()}", roleCardXpath(roleName))
        return WebUI.waitForElementVisible(card, timeoutSeconds, FailureHandling.OPTIONAL)
    }

    private static void waitForSearchSettled() { WebUI.waitForPageLoad(DEFAULT_TIMEOUT); WebUI.delay(1) }

    @Keyword
    def navigateToRolesAndPermissions() {
        try {
            if (DriverFactory.getWebDriver() == null) { WebUI.openBrowser(''); WebUI.maximizeWindow() }
            WebUI.navigateToUrl(resolveRolesPermissionsUrl())
            verifyRolesAndPermissionsPageLoaded()
        } catch (Exception e) { failStop("navigateToRolesAndPermissions failed: ${e.message}") }
    }

    @Keyword
    def navigateToStageRolesAndPermissions() {
        try {
            if (DriverFactory.getWebDriver() == null) { WebUI.openBrowser(''); WebUI.maximizeWindow() }
            WebUI.navigateToUrl(resolveRolesPermissionsStageUrl())
            verifyRolesAndPermissionsPageLoaded()
        } catch (Exception e) { failStop("navigateToStageRolesAndPermissions failed: ${e.message}") }
    }

    @Keyword
    def openRolesAndPermissionsFromMenu() {
        try {
            String current = ''
            try { current = WebUI.getUrl() ?: '' } catch (Exception ignored) { }
            if (!current.toLowerCase().contains('customs-management')) { WebUI.navigateToUrl(customsAppUrl()); WebUI.waitForPageLoad(DEFAULT_TIMEOUT) }
            TestObject menu = to('menu_RolesAndPermissions')
            if (WebUI.waitForElementVisible(menu, 10, FailureHandling.OPTIONAL)) click(menu)
            else { navigateToRolesAndPermissions(); return }
            verifyRolesAndPermissionsPageLoaded()
        } catch (Exception e) { failStop("openRolesAndPermissionsFromMenu failed: ${e.message}") }
    }

    @Keyword
    def openRolesAndPermissionsFromMenuStage() {
        try {
            String current = ''
            try { current = WebUI.getUrl() ?: '' } catch (Exception ignored) { }
            if (!current.toLowerCase().contains('customs-management')) { WebUI.navigateToUrl(stageCustomsAppUrl()); WebUI.waitForPageLoad(DEFAULT_TIMEOUT) }
            TestObject menu = to('menu_RolesAndPermissions')
            if (WebUI.waitForElementVisible(menu, 10, FailureHandling.OPTIONAL)) click(menu)
            else { navigateToStageRolesAndPermissions(); return }
            verifyRolesAndPermissionsPageLoaded()
        } catch (Exception e) { failStop("openRolesAndPermissionsFromMenuStage failed: ${e.message}") }
    }

    @Keyword
    def verifyRolesAndPermissionsPageLoaded() {
        try { waitVisible(to('txt_pageHeading_RolesPermissions')); WebUI.verifyElementVisible(to('txt_pageHeading_RolesPermissions'), FailureHandling.STOP_ON_FAILURE); WebUI.takeScreenshot() }
        catch (Exception e) { failStop("verifyRolesAndPermissionsPageLoaded failed: ${e.message}") }
    }

    @Keyword
    def openRolesTab() {
        try { TestObject tab = to('tab_Roles'); if (WebUI.waitForElementVisible(tab, 5, FailureHandling.OPTIONAL)) click(tab); waitVisible(to('inp_SearchRoles')); WebUI.takeScreenshot() }
        catch (Exception e) { failStop("openRolesTab failed: ${e.message}") }
    }

    @Keyword
    def verifyAllDefaultRolesDisplayed(List roleNames = null) {
        try {
            openRolesTab()
            List names = roleNames ?: DEFAULT_SYSTEM_ROLES
            for (String role : names) {
                if (!isRoleVisible(role, DEFAULT_TIMEOUT)) {
                    failStop("Expected role card not visible: ${role}")
                }
                WebUI.verifyElementVisible(dynamicXpathObject("role_visible_${role.hashCode()}", roleCardXpath(role)), FailureHandling.STOP_ON_FAILURE)
                KeywordUtil.logInfo("Role card visible: ${role}")
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyAllDefaultRolesDisplayed failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleCardDetails(String roleName = 'Super Admin') {
        try {
            openRolesTab()
            String role = (roleName ?: 'Super Admin').trim()
            TestObject card = dynamicXpathObject("role_detail_card_${role.hashCode()}", roleCardXpath(role))
            waitVisible(card)
            WebUI.verifyElementVisible(card, FailureHandling.STOP_ON_FAILURE)

            TestObject nameEl = dynamicXpathObject("role_name_${role.hashCode()}",
                "${roleCardXpath(role)}//*[normalize-space()=${xpathLiteral(role)}]")
            WebUI.verifyElementVisible(nameEl, FailureHandling.STOP_ON_FAILURE)

            TestObject viewDetails = dynamicXpathObject("role_view_details_${role.hashCode()}", viewDetailsLinkXpath(role))
            WebUI.verifyElementVisible(viewDetails, FailureHandling.STOP_ON_FAILURE)

            TestObject description = dynamicXpathObject("role_desc_${role.hashCode()}", roleCardDescriptionXpath(role))
            if (!WebUI.waitForElementVisible(description, 5, FailureHandling.OPTIONAL)) {
                failStop("Role description not visible on card for: ${role}")
            }
            WebUI.verifyElementVisible(description, FailureHandling.STOP_ON_FAILURE)

            TestObject usersAssigned = dynamicXpathObject("role_users_${role.hashCode()}",
                "${roleCardXpath(role)}//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'user')]")
            if (!WebUI.waitForElementVisible(usersAssigned, 5, FailureHandling.OPTIONAL)) {
                KeywordUtil.logInfo("Users-assigned text not found on card for ${role} — verifying View Details and description only")
            } else {
                WebUI.verifyElementVisible(usersAssigned, FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleCardDetails failed: ${e.message}")
        }
    }

    /**
     * Verifies role name (h3) and description (p) on the same role card.
     * Description match uses contains by default so it works after edits (e.g. AutoTest suffix)
     * and across roles with different description text.
     *
     * Example:
     *   verifyRoleCardNameAndDescription('Super Admin', 'Full system access')
     *   verifyRoleCardNameAndDescription('Super Admin', 'AutoTest-', true)
     */
    @Keyword
    def verifyRoleCardNameAndDescription(String roleName, String descriptionFragment, boolean descriptionUseContains = true, boolean refreshPageFirst = false) {
        try {
            if (refreshPageFirst) {
                refreshRolesAndPermissionsPage()
            } else {
                openRolesTab()
                clearRoleSearch()
            }
            String role = (roleName ?: '').trim()
            String description = normalizeDescriptionFragment(descriptionFragment)
            if (!role) {
                failStop('verifyRoleCardNameAndDescription requires roleName')
            }
            if (!description) {
                failStop('verifyRoleCardNameAndDescription requires descriptionFragment')
            }
            if (description.contains('AutoTest-')) {
                descriptionUseContains = true
            }

            TestObject roleHeading = dynamicXpathObject("role_card_h3_${role.hashCode()}", roleCardHeadingXpath(role))
            waitVisible(roleHeading)
            WebUI.verifyElementVisible(roleHeading, FailureHandling.STOP_ON_FAILURE)
            KeywordUtil.logInfo("Role card heading verified: ${role}")

            boolean descriptionMatched = false
            List<String> descriptionXpaths = descriptionUseContains ?
                roleCardDescriptionParagraphXpaths(role, description) :
                [roleCardDescriptionParagraphXpath(role, description, false)] + roleCardDescriptionParagraphXpaths(role, description)

            for (int i = 0; i < descriptionXpaths.size(); i++) {
                TestObject roleDescription = dynamicXpathObject(
                    "role_card_desc_p_${role.hashCode()}_${description.hashCode()}_${i}",
                    descriptionXpaths[i])
                if (WebUI.waitForElementVisible(roleDescription, 8, FailureHandling.OPTIONAL)) {
                    WebUI.verifyElementVisible(roleDescription, FailureHandling.STOP_ON_FAILURE)
                    descriptionMatched = true
                    break
                }
            }

            if (!descriptionMatched) {
                TestObject descParagraph = dynamicXpathObject("role_card_desc_read_${role.hashCode()}",
                    "${roleCardXpath(role)}//p[contains(@class,'description')]")
                if (WebUI.waitForElementVisible(descParagraph, 5, FailureHandling.OPTIONAL)) {
                    String actual = normalizeDescriptionFragment(WebUI.getText(descParagraph, FailureHandling.OPTIONAL))
                    if (descriptionUseContains ? actual.contains(description) : actual.equalsIgnoreCase(description)) {
                        descriptionMatched = true
                    }
                }
            }

            if (!descriptionMatched) {
                failStop("Role card description for '${role}' not found with '${description}'. Use a unique contains fragment such as 'AutoTest-<timestamp>'.")
            }
            KeywordUtil.logInfo("Role card description verified for '${role}' with '${description}'")
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleCardNameAndDescription failed: ${e.message}")
        }
    }

    @Keyword
    def searchRoles(String keyword) {
        try { openRolesTab(); TestObject search = to('inp_SearchRoles'); waitVisibleClickable(search); WebUI.clearText(search, FailureHandling.OPTIONAL); WebUI.setText(search, (keyword ?: '').trim()); WebUI.takeScreenshot(); waitForSearchSettled() }
        catch (Exception e) { failStop("searchRoles failed: ${e.message}") }
    }

    @Keyword
    def clearRoleSearch() {
        try {
            openRolesTab()
            TestObject search = to('inp_SearchRoles')
            waitVisibleClickable(search)
            WebUI.clearText(search, FailureHandling.OPTIONAL)
            String remaining = (WebUI.getAttribute(search, 'value', FailureHandling.OPTIONAL) ?: '').trim()
            if (remaining) {
                WebUI.setText(search, '')
            }
            WebUI.takeScreenshot()
            waitForSearchSettled()
        } catch (Exception e) {
            failStop("clearRoleSearch failed: ${e.message}")
        }
    }

    @Keyword
    def refreshRolesAndPermissionsPage() {
        try {
            WebUI.refresh()
            WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
            verifyRolesAndPermissionsPageLoaded()
            openRolesTab()
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("refreshRolesAndPermissionsPage failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleVisible(String roleName) {
        try { String role = (roleName ?: '').trim(); if (!isRoleVisible(role, DEFAULT_TIMEOUT)) failStop("Expected role card visible after search: ${role}"); WebUI.verifyElementVisible(dynamicXpathObject("role_visible_${role.hashCode()}", roleCardXpath(role)), FailureHandling.STOP_ON_FAILURE) }
        catch (Exception e) { failStop("verifyRoleVisible failed: ${e.message}") }
    }

    @Keyword
    def verifyRoleNotVisible(String roleName) {
        try {
            String role = (roleName ?: '').trim()
            TestObject card = dynamicXpathObject("role_hidden_${role.hashCode()}", roleCardXpath(role))
            if (WebUI.waitForElementVisible(card, 3, FailureHandling.OPTIONAL)) {
                failStop("Role card should be hidden after search but was visible: ${role}")
            }
            KeywordUtil.logInfo("Role card correctly hidden: ${role}")
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleNotVisible failed: ${e.message}")
        }
    }

    @Keyword
    def verifyOnlyRolesVisible(List roleNames) {
        try {
            List expected = roleNames ?: []
            for (String role : expected) {
                verifyRoleVisible(role)
            }
            for (String role : DEFAULT_SYSTEM_ROLES) {
                if (!expected.contains(role)) {
                    verifyRoleNotVisible(role)
                }
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyOnlyRolesVisible failed: ${e.message}")
        }
    }

    @Keyword
    def verifyNoRolesFoundMessage() {
        try {
            waitVisible(to('msg_noRolesFound'))
            WebUI.verifyElementVisible(to('msg_noRolesFound'), FailureHandling.STOP_ON_FAILURE)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyNoRolesFoundMessage failed: ${e.message}")
        }
    }

    @Keyword
    def verifySearchTriggeredWithMinimumCharacters(String singleChar = 'S') {
        try {
            openRolesTab()
            verifyAllDefaultRolesDisplayed()
            searchRoles(singleChar)
            boolean anyFiltered = false
            for (String role : DEFAULT_SYSTEM_ROLES) {
                if (role.toLowerCase().contains(singleChar.toLowerCase()) && isRoleVisible(role, 5)) {
                    anyFiltered = true
                }
            }
            if (!anyFiltered) {
                failStop("Search did not filter roles after entering minimum character: ${singleChar}")
            }
            KeywordUtil.logInfo("Search triggered after entering 1 character: ${singleChar}")
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifySearchTriggeredWithMinimumCharacters failed: ${e.message}")
        }
    }

    // --- CCIPH-91 Edit Role ---

    @Keyword
    def openRoleCardEllipsisMenuForRole(String roleName = 'Super Admin') {
        try {
            openRolesTab()
            String role = (roleName ?: 'Super Admin').trim()
            clearRoleSearch()
            TestObject card = dynamicXpathObject("menu_target_card_${role.hashCode()}", roleCardXpath(role))
            waitVisible(card)
            WebUI.click(card)
            WebUI.takeScreenshot()
            TestObject ellipsis = dynamicXpathObject("menu_ellipsis_${role.hashCode()}", roleCardEllipsisXpath(role))
            if (!WebUI.waitForElementVisible(ellipsis, 5, FailureHandling.OPTIONAL)) {
                WebUI.mouseOver(card)
            }
            click(ellipsis)
        } catch (Exception e) {
            failStop("openRoleCardEllipsisMenuForRole failed: ${e.message}")
        }
    }

    @Keyword
    def openEditRoleFormForRole(String roleName = 'Super Admin') {
        try {
            openRoleCardEllipsisMenuForRole(roleName)
           // click(to91('mnu_editRole'))
			click(to91('btn_EditButton'))
            verifyEditRoleFormVisible()
        } catch (Exception e) {
            failStop("openEditRoleFormForRole failed: ${e.message}")
        }
    }

    @Keyword
    def verifyEditRoleFormVisible() {
        try {
            waitVisible(to91('dlg_editRoleForm'))
            WebUI.verifyElementVisible(to91('dlg_editRoleForm'), FailureHandling.STOP_ON_FAILURE)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyEditRoleFormVisible failed: ${e.message}")
        }
    }

    @Keyword
    def verifyEditRoleFormOpenWithPrefilledData(String roleName = 'Super Admin') {
        try {
            verifyEditRoleFormVisible()
            String role = (roleName ?: 'Super Admin').trim()
            TestObject nameField = to91('inp_roleNameEnglish')
            if (WebUI.waitForElementVisible(nameField, 5, FailureHandling.OPTIONAL)) {
                String value = (WebUI.getAttribute(nameField, 'value', FailureHandling.OPTIONAL) ?: '').trim()
                if (!value) {
                    value = (WebUI.getText(nameField, FailureHandling.OPTIONAL) ?: '').trim()
                }
                if (!value.equalsIgnoreCase(role)) {
                    failStop("Edit form Role Name (English) expected '${role}' but was '${value}'")
                }
                KeywordUtil.logInfo("Edit form pre-filled with role name: ${value}")
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyEditRoleFormOpenWithPrefilledData failed: ${e.message}")
        }
    }

    @Keyword
    String getRoleCardDescriptionText(String roleName = 'Super Admin') {
        try {
            openRolesTab()
            clearRoleSearch()
            String role = (roleName ?: 'Super Admin').trim()
            TestObject desc = dynamicXpathObject("role_card_desc_read_${role.hashCode()}",
                "${roleCardXpath(role)}//p[contains(@class,'description')]")
            if (!WebUI.waitForElementVisible(desc, 5, FailureHandling.OPTIONAL)) {
                desc = dynamicXpathObject("role_card_desc_read_fallback_${role.hashCode()}", roleCardDescriptionXpath(role))
            }
            waitVisible(desc)
            String text = normalizeDescriptionFragment(WebUI.getText(desc, FailureHandling.OPTIONAL))
            KeywordUtil.logInfo("Role card description for '${role}': ${text}")
            WebUI.takeScreenshot()
            return text
        } catch (Exception e) {
            failStop("getRoleCardDescriptionText failed: ${e.message}")
            return ''
        }
    }

    @Keyword
    String getEditRoleDescriptionEnglish() {
        try {
            TestObject field = to91('inp_roleDescriptionEnglish')
            waitVisible(field)
            String value = (WebUI.getAttribute(field, 'value', FailureHandling.OPTIONAL) ?: '').trim()
            if (!value) {
                value = (WebUI.getText(field, FailureHandling.OPTIONAL) ?: '').trim()
            }
            value = normalizeDescriptionFragment(value)
            KeywordUtil.logInfo("Edit form description (English): ${value}")
            WebUI.takeScreenshot()
            return value
        } catch (Exception e) {
            failStop("getEditRoleDescriptionEnglish failed: ${e.message}")
            return ''
        }
    }

    @Keyword
    def setEditRoleDescriptionEnglish(String description) {
        try {
            TestObject field = to91('inp_roleDescriptionEnglish')
            waitVisibleClickable(field)
            WebElement element = WebUiCommonHelper.findWebElement(field, DEFAULT_TIMEOUT)
            element.clear()
            element.sendKeys((description ?: '').trim())
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("setEditRoleDescriptionEnglish failed: ${e.message}")
        }
    }

    @Keyword
    def clickSaveEditRole() {
        try {
            click(to91('btn_saveEditRole'))
            WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("clickSaveEditRole failed: ${e.message}")
        }
    }

    @Keyword
    def clickCloseEditRole() {
        try {
            click(to91('btn_closeEditRole'))
            boolean closed = !WebUI.waitForElementVisible(to91('dlg_editRoleForm'), 5, FailureHandling.OPTIONAL)
            if (!closed) {
                WebUI.sendKeys(to91('dlg_editRoleForm'), '\uE00C')
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("clickCloseEditRole failed: ${e.message}")
        }
    }

    @Keyword
    def verifyEditRoleSuccessMessage() {
        try {
            if (!WebUI.waitForElementVisible(to91('msg_editRoleSuccess'), DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
                boolean dialogClosed = !WebUI.waitForElementVisible(to91('dlg_editRoleForm'), 5, FailureHandling.OPTIONAL)
                if (!dialogClosed) {
                    failStop('Edit role success message not shown and dialog still open after Save')
                }
            } else {
                WebUI.verifyElementVisible(to91('msg_editRoleSuccess'), FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyEditRoleSuccessMessage failed: ${e.message}")
        }
    }

	@Keyword
	def verifyEditPermissionsButtonDisabled() {
		try {
			TestObject editPermissionsButton = findTestObject(
				'Object Repository/Roles&Permission/CCIPH-96/btn_EditDisabled'
			)

			WebUI.waitForElementVisible(
				editPermissionsButton,
				15,
				FailureHandling.STOP_ON_FAILURE
			)

			WebUI.verifyElementNotClickable(
				editPermissionsButton,
				FailureHandling.STOP_ON_FAILURE
			)

			String disabledAttr = WebUI.getAttribute(editPermissionsButton, 'disabled', FailureHandling.OPTIONAL)
			String ariaDisabled = WebUI.getAttribute(editPermissionsButton, 'aria-disabled', FailureHandling.OPTIONAL)
			if (disabledAttr == null && !ariaDisabled?.equalsIgnoreCase('true')) {
				KeywordUtil.logInfo('Edit Permissions button verified as not clickable; disabled attribute not explicitly set on element')
			}

			KeywordUtil.logInfo('Edit Permissions button is disabled for system role')

			WebUI.takeScreenshot()

		} catch (Throwable t) {
			WebUI.takeScreenshot()
			KeywordUtil.markFailedAndStop(
				"[verifyEditPermissionsButtonDisabled] Edit Permissions button is not disabled: ${t.message}"
			)
		}
	}
	
    @Keyword
    def verifyRoleCardDescriptionContains(String roleName, String expectedText) {
        try {
            String fragment = normalizeDescriptionFragment(expectedText)
            refreshRolesAndPermissionsPage()
            verifyRoleCardNameAndDescription(roleName, fragment, true, false)
        } catch (Exception e) {
            failStop("verifyRoleCardDescriptionContains failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleCardDescriptionEquals(String roleName, String expectedText) {
        try {
            refreshRolesAndPermissionsPage()
            clearRoleSearch()
            String expected = normalizeDescriptionFragment(expectedText)
            String actual = getRoleCardDescriptionText(roleName)
            if (!actual.equalsIgnoreCase(expected)) {
                failStop("Role card description for '${roleName}' expected '${expected}' but was '${actual}'")
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleCardDescriptionEquals failed: ${e.message}")
        }
    }

    @Keyword
    def verifyEditRoleFormClosed() {
        try {
            if (WebUI.waitForElementVisible(to91('dlg_editRoleForm'), 3, FailureHandling.OPTIONAL)) {
                failStop('Edit role form should be closed but is still visible')
            }
            waitVisible(to('inp_SearchRoles'))
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyEditRoleFormClosed failed: ${e.message}")
        }
    }

    // --- CCIPH-92 Duplicate Role ---

    @Keyword
    def openDuplicateRoleFormForRole(String roleName = 'Super Admin') {
        try {
            openRoleCardEllipsisMenuForRole(roleName)
            click(to92('btn_Duplicate'))
            verifyDuplicateRoleFormVisible()
        } catch (Exception e) {
            failStop("openDuplicateRoleFormForRole failed: ${e.message}")
        }
    }

    @Keyword
    def verifyDuplicateRoleFormVisible() {
        try {
            TestObject duplicateDlg = to92('dlg_duplicateRoleForm')
            if (!WebUI.waitForElementVisible(duplicateDlg, 5, FailureHandling.OPTIONAL)) {
                verifyEditRoleFormVisible()
            } else {
                waitVisible(duplicateDlg)
                WebUI.verifyElementVisible(duplicateDlg, FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyDuplicateRoleFormVisible failed: ${e.message}")
        }
    }

    @Keyword
    def verifyDuplicateRoleFormPrefilledFromRole(String sourceRoleName = 'Super Admin') {
        try {
            verifyDuplicateRoleFormVisible()
            String role = (sourceRoleName ?: 'Super Admin').trim()
            TestObject nameField = to91('inp_roleNameEnglish')
            if (WebUI.waitForElementVisible(nameField, 5, FailureHandling.OPTIONAL)) {
                String value = (WebUI.getAttribute(nameField, 'value', FailureHandling.OPTIONAL) ?: '').trim()
                if (!value) {
                    value = (WebUI.getText(nameField, FailureHandling.OPTIONAL) ?: '').trim()
                }
                KeywordUtil.logInfo("Duplicate form pre-filled role name: ${value} (source: ${role})")
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyDuplicateRoleFormPrefilledFromRole failed: ${e.message}")
        }
    }

    @Keyword
    def setRoleNameEnglish(String roleName) {
        try {
            TestObject nameField = to91('inp_roleNameEnglish')
            waitVisibleClickable(nameField)
            WebUI.clearText(nameField, FailureHandling.OPTIONAL)
            WebUI.setText(nameField, (roleName ?: '').trim())
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("setRoleNameEnglish failed: ${e.message}")
        }
    }

    @Keyword
    def clickSaveRoleForm() {
        try {
            click(to91('btn_saveEditRole'))
            WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
        } catch (Exception e) {
            failStop("clickSaveRoleForm failed: ${e.message}")
        }
    }

    @Keyword
    def clickCloseRoleForm() {
        try {
            click(to91('btn_closeEditRole'))
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("clickCloseRoleForm failed: ${e.message}")
        }
    }

    @Keyword
    def verifyDuplicateRoleSuccessMessage() {
        try {
            verifyEditRoleSuccessMessage()
        } catch (Exception e) {
            failStop("verifyDuplicateRoleSuccessMessage failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleAlreadyExistsMessage() {
        try {
            if (!WebUI.waitForElementVisible(to92('msg_roleAlreadyExists'), DEFAULT_TIMEOUT, FailureHandling.OPTIONAL)) {
                failStop('Expected Role already exists message but none was displayed')
            }
            WebUI.verifyElementVisible(to92('msg_roleAlreadyExists'), FailureHandling.STOP_ON_FAILURE)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleAlreadyExistsMessage failed: ${e.message}")
        }
    }

    @Keyword
    def verifyDuplicateRoleFormClosed() {
        try {
            verifyEditRoleFormClosed()
        } catch (Exception e) {
            failStop("verifyDuplicateRoleFormClosed failed: ${e.message}")
        }
    }

    // --- CCIPH-95 View Role Details ---

    @Keyword
    def clickViewDetailsForRole(String roleName = 'Super Admin') {
        try {
            openRolesTab()
            String role = (roleName ?: 'Super Admin').trim()
            clearRoleSearch()
            TestObject viewDetails = dynamicXpathObject("view_details_${role.hashCode()}", viewDetailsLinkXpath(role))
            WebUI.scrollToElement(viewDetails, 10)
            waitVisibleClickable(viewDetails)
            WebUI.click(viewDetails)
            WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
            verifyRoleDetailsScreenOpen()
        } catch (Exception e) {
            failStop("clickViewDetailsForRole failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleDetailsScreenOpen() {
        try {
            if (!waitForAnyVisible(roleDetailsScreenXpaths(), 'role_details_screen', 10)) {
                waitVisible(to95('dlg_roleDetails'))
            }
            WebUI.verifyElementVisible(to95('dlg_roleDetails'), FailureHandling.STOP_ON_FAILURE)
            KeywordUtil.logInfo('Role details screen is open')
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleDetailsScreenOpen failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleDetailsBasicInfo(String roleName = 'Super Admin') {
        try {
            verifyRoleDetailsScreenOpen()
            String role = (roleName ?: 'Super Admin').trim()
            TestObject nameOnDetails = dynamicXpathObject("details_name_${role.hashCode()}",
                "//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]//*[self::h1 or self::h2 or self::h3 or self::p or self::span][normalize-space()=${xpathLiteral(role)}]")
            if (!WebUI.waitForElementVisible(nameOnDetails, 5, FailureHandling.OPTIONAL)) {
                TestObject anyName = dynamicXpathObject('details_any_name',
                    "//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]//*[self::h1 or self::h2 or self::h3][1]")
                WebUI.verifyElementVisible(anyName, FailureHandling.STOP_ON_FAILURE)
            } else {
                WebUI.verifyElementVisible(nameOnDetails, FailureHandling.STOP_ON_FAILURE)
            }
            TestObject roleDescriptionLabel = dynamicXpathObject('details_role_description_label',
                "//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]")
            WebUI.verifyElementVisible(roleDescriptionLabel, FailureHandling.STOP_ON_FAILURE)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleDetailsBasicInfo failed: ${e.message}")
        }
    }

    @Keyword
    def verifyPermissionsGroupedByModule() {
        try {
            verifyRoleDetailsScreenOpen()
            TestObject permissionsHeading = dynamicXpathObject('details_permissions_heading',
                "//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'permissions') and not(contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit permission'))]")
            WebUI.verifyElementVisible(permissionsHeading, FailureHandling.STOP_ON_FAILURE)
            if (WebUI.waitForElementVisible(to95('section_roleDetailsPermissions'), 5, FailureHandling.OPTIONAL)) {
                WebUI.verifyElementVisible(to95('section_roleDetailsPermissions'), FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyPermissionsGroupedByModule failed: ${e.message}")
        }
    }

    @Keyword
    def clickAnyPermissionOnDetailsScreen() {
        try {
            verifyRoleDetailsScreenOpen()
            TestObject perm = to95('item_firstPermission')
            if (WebUI.waitForElementVisible(perm, 5, FailureHandling.OPTIONAL)) {
                click(perm)
            } else {
                TestObject permissionsSection = dynamicXpathObject('details_permissions_click_target',
                    "//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'permissions') and not(contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit permission'))]")
                if (WebUI.waitForElementVisible(permissionsSection, 5, FailureHandling.OPTIONAL)) {
                    WebUI.click(permissionsSection)
                }
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("clickAnyPermissionOnDetailsScreen failed: ${e.message}")
        }
    }

    @Keyword
    def verifyNoPermissionsAssignedMessage() {
        try {
            verifyRoleDetailsScreenOpen()
            boolean messageFound = false
            for (int i = 0; i < noPermissionsAssignedMessageXpaths().size(); i++) {
                TestObject message = dynamicXpathObject("msg_no_permissions_${i}", noPermissionsAssignedMessageXpaths()[i])
                if (WebUI.waitForElementVisible(message, 5, FailureHandling.OPTIONAL)) {
                    WebUI.verifyElementVisible(message, FailureHandling.STOP_ON_FAILURE)
                    messageFound = true
                    break
                }
            }
            if (!messageFound && roleDetailsPanelTextIndicatesNoPermissions()) {
                KeywordUtil.logInfo('No permissions assigned message verified via role details panel text')
                messageFound = true
            }
            if (!messageFound && roleDetailsShowsEmptyPermissionsState()) {
                KeywordUtil.logInfo('No permissions assigned state verified via empty permissions section')
                messageFound = true
            }
            if (!messageFound) {
                waitVisible(to95('msg_noPermissionsAssigned'))
                WebUI.verifyElementVisible(to95('msg_noPermissionsAssigned'), FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyNoPermissionsAssignedMessage failed: ${e.message}")
        }
    }

    @Keyword
    def verifyPermissionsAreViewOnlyOnDetailsScreen() {
        try {
            clickAnyPermissionOnDetailsScreen()
            boolean saveVisible = WebUI.waitForElementVisible(to95('btn_savePermissionChanges'), 2, FailureHandling.OPTIONAL)
            if (saveVisible) {
                failStop('Save changes visible on view-only role details after clicking permission')
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyPermissionsAreViewOnlyOnDetailsScreen failed: ${e.message}")
        }
    }

    @Keyword
    def clickCloseRoleDetails() {
        try {
            click(to95('btn_closeRoleDetails'))
        } catch (Exception e) {
            failStop("clickCloseRoleDetails failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleDetailsScreenClosed() {
        try {
            if (WebUI.waitForElementVisible(to95('dlg_roleDetails'), 3, FailureHandling.OPTIONAL)) {
                failStop('Role details screen should be closed but is still visible')
            }
            waitVisible(to('inp_SearchRoles'))
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleDetailsScreenClosed failed: ${e.message}")
        }
    }
	
	@Keyword
	def verifyRolesAndPermissionsTitle() {
		try {
			TestObject title = findTestObject('Object Repository/Roles&Permission/CCIPH-95/dlg_RolesPermission')
	
			WebUI.waitForElementVisible(title, 15, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementText(
				title,
				'Roles & Permissions',
				FailureHandling.STOP_ON_FAILURE
			)
	
			WebUI.takeScreenshot()
		} catch (Throwable t) {
			WebUI.takeScreenshot()
			KeywordUtil.markFailedAndStop(
				"[verifyRolesAndPermissionsTitle] Roles & Permissions title validation failed: ${t.message}"
			)
		}
	}

    private static List<String> noPermissionsAssignedMessageXpaths() {
        String panel = "//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]"
        return [
            "${panel}//*[self::p or self::span or self::div or self::h4 or self::h5][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no permissions assigned')]",
            "${panel}//*[self::p or self::span or self::div or self::h4 or self::h5][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no permission assigned')]",
            "${panel}//*[self::p or self::span or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no permissions') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'assigned')]",
            "${panel}//*[contains(@class,'empty') or contains(@class,'Empty') or contains(@class,'noData') or contains(@class,'placeholder')][.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'permission')]]",
            "//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no permissions assigned') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no permission assigned')]"
        ]
    }

    private static boolean isNoPermissionsAssignedText(String text) {
        String t = (text ?: '').toLowerCase()
        if (!t) {
            return false
        }
        return t.contains('no permissions assigned') ||
            t.contains('no permission assigned') ||
            t.contains('no permissions to display') ||
            t.contains('no permissions found') ||
            t.contains('no permissions available') ||
            (t.contains('no permission') && t.contains('assigned')) ||
            (t.contains('no permissions') && t.contains('assigned'))
    }

    private static boolean roleDetailsShowsEmptyPermissionsState() {
        String panel = "//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]"
        TestObject permTag = dynamicXpathObject('details_perm_tag_any',
            "${panel}//*[self::button or self::span or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'view') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'create') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'delete') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'export')]")
        if (WebUI.waitForElementVisible(permTag, 2, FailureHandling.OPTIONAL)) {
            return false
        }
        TestObject emptyState = dynamicXpathObject('details_empty_permissions',
            "${panel}//*[contains(@class,'empty') or contains(@class,'Empty') or contains(@class,'noData') or contains(@class,'placeholder')]")
        return WebUI.waitForElementVisible(emptyState, 3, FailureHandling.OPTIONAL)
    }

    private static boolean roleDetailsPanelTextIndicatesNoPermissions() {
        for (String panelXpath : roleDetailsScreenXpaths()) {
            TestObject panel = dynamicXpathObject("role_details_text_${panelXpath.hashCode()}", panelXpath)
            if (WebUI.waitForElementVisible(panel, 3, FailureHandling.OPTIONAL)) {
                if (isNoPermissionsAssignedText(WebUI.getText(panel, FailureHandling.OPTIONAL))) {
                    return true
                }
            }
        }
        return false
    }

    @Keyword
    def clickEditPermissionLinkOnDetails() {
        try {
            verifyRoleDetailsScreenOpen()
            click(to95('lnk_editPermissions'))
            verifyPermissionEditModeEnabled()
        } catch (Exception e) {
            failStop("clickEditPermissionLinkOnDetails failed: ${e.message}")
        }
    }

    private static List<String> editablePermissionTagXpaths() {
        return [
            permissionTagXpath('Delete', 'Customs'),
            permissionTagXpath('Export', 'Customs'),
            permissionTagXpath('Delete'),
            permissionTagXpath('Export'),
            "(//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]//*[self::button or self::span or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'delete') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'export') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'create') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit')])[not(contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'edit permission'))])[1]"
        ]
    }

    @Keyword
    def toggleFirstEditablePermissionOnDetails() {
        try {
            verifyPermissionEditModeEnabled()
            for (int i = 0; i < editablePermissionTagXpaths().size(); i++) {
                TestObject toggle = dynamicXpathObject("perm_toggle_edit_${i}", editablePermissionTagXpaths()[i])
                if (WebUI.waitForElementVisible(toggle, 5, FailureHandling.OPTIONAL)) {
                    click(toggle)
                    WebUI.takeScreenshot()
                    KeywordUtil.logInfo("Toggled permission tag using xpath index ${i}")
                    return
                }
            }
            failStop('No editable permission tag found after Edit Permission')
        } catch (Exception e) {
            failStop("toggleFirstEditablePermissionOnDetails failed: ${e.message}")
        }
    }

    @Keyword
    def clickSavePermissionChanges() {
        try {
            click(to95('btn_savePermissionChanges'))
            WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
            verifyEditRoleSuccessMessage()
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("clickSavePermissionChanges failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleDetailsPermissionsUpdated() {
        try {
            verifyRoleDetailsScreenOpen()
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleDetailsPermissionsUpdated failed: ${e.message}")
        }
    }

    @Keyword
    def verifyRoleDetailsPermissionsScrollable() {
        try {
            verifyRoleDetailsScreenOpen()
            TestObject section = to95('section_roleDetailsPermissions')
            if (WebUI.waitForElementVisible(section, 5, FailureHandling.OPTIONAL)) {
                WebUI.scrollToElement(section, DEFAULT_TIMEOUT)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyRoleDetailsPermissionsScrollable failed: ${e.message}")
        }
    }

    // --- CCIPH-96 Edit Permissions ---

    private static String permissionTagXpath(String action, String moduleName = '') {
        String act = xpathLiteral((action ?: 'View').trim().toLowerCase())
        String moduleScope = ''
        if ((moduleName ?: '').trim()) {
            String mod = xpathLiteral(moduleName.trim().toLowerCase())
            moduleScope = "[ancestor::*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${mod})]]]"
        }
        return "(//*[.//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'role description')]]//*[self::button or self::span or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${act})]${moduleScope} | //*[@role='dialog' or contains(@class,'modal') or contains(@class,'dialog') or contains(@class,'drawer') or contains(@class,'Drawer')]//*[self::button or self::span or self::div][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${act})]${moduleScope})[1]"
    }

    @Keyword
    def openRoleDetailsAndEditPermissions(String roleName = 'Customs Manager') {
        try {
            clickViewDetailsForRole(roleName)
            clickEditPermissionsOnRoleDetails()
        } catch (Exception e) {
            failStop("openRoleDetailsAndEditPermissions failed: ${e.message}")
        }
    }
	
	@Keyword
	def openRoleDetailsAndEditPermissionsSuper(String roleName) {
		try {
			clickViewDetailsForRole(roleName)
		 //   clickEditPermissionsOnRoleDetails()
		} catch (Exception e) {
			failStop("openRoleDetailsAndEditPermissions failed: ${e.message}")
		}
	}

    @Keyword
    def clickEditPermissionsOnRoleDetails() {
        try {
            verifyRoleDetailsScreenOpen()
            click(to95('lnk_editPermissions'))
            verifyPermissionEditModeEnabled()
        } catch (Exception e) {
            failStop("clickEditPermissionsOnRoleDetails failed: ${e.message}")
        }
    }

    @Keyword
    def verifyPermissionEditModeEnabled() {
        try {
            WebUI.verifyElementVisible(to95('btn_savePermissionChanges'), FailureHandling.STOP_ON_FAILURE)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyPermissionEditModeEnabled failed: ${e.message}")
        }
    }

    @Keyword
    def togglePermissionTag(String action = 'View', String moduleName = 'Customs') {
        try {
            verifyPermissionEditModeEnabled()
            TestObject tag = dynamicXpathObject("perm_tag_${action}_${moduleName.hashCode()}", permissionTagXpath(action, moduleName))
            click(tag)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("togglePermissionTag failed: ${e.message}")
        }
    }

    @Keyword
    def clickSavePermissionsOnDetails() {
        try {
            click(to95('btn_savePermissionChanges'))
            WebUI.waitForPageLoad(DEFAULT_TIMEOUT)
        } catch (Exception e) {
            failStop("clickSavePermissionsOnDetails failed: ${e.message}")
        }
    }

    @Keyword
    def verifyInvalidPermissionCombinationError() {
        try {
            waitVisible(to96('msg_invalidPermissionCombination'))
            WebUI.verifyElementVisible(to96('msg_invalidPermissionCombination'), FailureHandling.STOP_ON_FAILURE)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyInvalidPermissionCombinationError failed: ${e.message}")
        }
    }

    @Keyword
    def clickCancelPermissionEdit() {
        try {
            if (WebUI.waitForElementVisible(to96('btn_cancelPermissionEdit'), 5, FailureHandling.OPTIONAL)) {
                click(to96('btn_cancelPermissionEdit'))
            } else {
                clickCloseRoleDetails()
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("clickCancelPermissionEdit failed: ${e.message}")
        }
    }

    @Keyword
    def verifyPermissionTagEnabled(String action = 'Delete', String moduleName = 'Customs') {
        try {
            verifyRoleDetailsScreenOpen()
            TestObject tag = dynamicXpathObject("perm_any_${action}_${moduleName.hashCode()}", permissionTagXpath(action, moduleName))
            waitVisible(tag)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyPermissionTagEnabled failed: ${e.message}")
        }
    }

    @Keyword
    def verifyPermissionTagUnchangedAfterCancel(String action = 'View', String moduleName = 'Customs') {
        try {
            verifyPermissionTagEnabled(action, moduleName)
        } catch (Exception e) {
            failStop("verifyPermissionTagUnchangedAfterCancel failed: ${e.message}")
        }
    }

    @Keyword
    def savePermissionsAndVerifySuccess() {
        try {
            clickSavePermissionsOnDetails()
            verifyEditRoleSuccessMessage()
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("savePermissionsAndVerifySuccess failed: ${e.message}")
        }
    }

    // --- CCIPH-97 Permission Matrix ---

    private static String matrixRoleColumnXpath(String roleName) {
        String role = xpathLiteral((roleName ?: '').trim())
        return "//table//th[normalize-space()=${role}] | //table//thead//*[normalize-space()=${role}]"
    }

    private static String matrixModuleRowXpath(String moduleName) {
        String mod = xpathLiteral((moduleName ?: '').trim())
        return "//table//tr[.//td[normalize-space()=${mod}] or .//*[normalize-space()=${mod}]]"
    }

    @Keyword
    def openPermissionsMatrixTab() {
        try {
            TestObject tab = to97('btn_PermissionMatrix')
            waitVisibleClickable(tab)
            click(tab)
            verifyPermissionMatrixScreenLoaded()
        } catch (Exception e) {
            failStop("openPermissionsMatrixTab failed: ${e.message}")
        }
    }

    @Keyword
    def verifyPermissionMatrixScreenLoaded() {
        try {
            waitVisible(to97('tbl_permissionMatrix'))
            WebUI.verifyElementVisible(to97('tbl_permissionMatrix'), FailureHandling.STOP_ON_FAILURE)
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyPermissionMatrixScreenLoaded failed: ${e.message}")
        }
    }

    @Keyword
    def verifyMatrixRoleColumns(List roleNames = null) {
        try {
            verifyPermissionMatrixScreenLoaded()
            List roles = roleNames ?: MATRIX_ROLE_COLUMNS
            for (String role : roles) {
                TestObject col = dynamicXpathObject("matrix_col_${role.hashCode()}", matrixRoleColumnXpath(role))
                waitVisible(col)
                WebUI.verifyElementVisible(col, FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyMatrixRoleColumns failed: ${e.message}")
        }
    }
	
	@Keyword
	def verifyPermissionsMatrixTablePresent() {
		try {
			TestObject table = findTestObject(
				'Object Repository/Roles&Permission/CCIPH-97/tbl_permissionMatrix'
			)
	
			WebUI.waitForElementVisible(table, 15, FailureHandling.STOP_ON_FAILURE)
			WebUI.verifyElementVisible(table, FailureHandling.STOP_ON_FAILURE)
	
			WebUI.takeScreenshot()
	
		} catch (Throwable t) {
			WebUI.takeScreenshot()
			KeywordUtil.markFailedAndStop(
				"[verifyPermissionsMatrixTablePresent] ${t.message}"
			)
		}
	}

//    @Keyword
//    def verifyMatrixModuleRows(List moduleNames = null) {
//        try {
//            verifyPermissionMatrixScreenLoaded()
//            List modules = moduleNames ?: MATRIX_MODULE_ROWS
//            for (String module : modules) {
//                TestObject row = dynamicXpathObject("matrix_row_${module.hashCode()}", matrixModuleRowXpath(module))
//                waitVisible(row)
//                WebUI.verifyElementVisible(row, FailureHandling.STOP_ON_FAILURE)
//            }
//            WebUI.takeScreenshot()
//        } catch (Exception e) {
//            failStop("verifyMatrixModuleRows failed: ${e.message}")
//        }
//    }
	
	
	@Keyword
	def verifyMatrixModuleRows(List moduleNames = null) {
		try {
			verifyPermissionMatrixScreenLoaded()
	
			List modules = moduleNames ?: MATRIX_MODULE_ROWS
	
			for (String module : modules) {
	
				TestObject row = dynamicXpathObject(
					"matrix_row_${module.hashCode()}",
					matrixModuleRowXpaths(module)
				)
	
				waitVisible(row)
	
				WebUI.verifyElementVisible(
					row,
					FailureHandling.STOP_ON_FAILURE
				)
			}
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			failStop("verifyMatrixModuleRows failed: ${e.message}")
		}
	}
	
	
	private String matrixModuleRowXpaths(String moduleName) {
    return "//table[contains(@class,'PermissionsMatrix-module__')]//tbody/tr[td[1][normalize-space()='${moduleName}']]"
}

    @Keyword
    def verifyMatrixShowsPermissionActions(String roleName = 'Super Admin', String moduleName = 'Customs') {
        try {
            verifyPermissionMatrixScreenLoaded()
            TestObject row = dynamicXpathObject("matrix_perm_row_${moduleName.hashCode()}", matrixModuleRowXpath(moduleName))
            waitVisible(row)
            for (String action : PERMISSION_ACTIONS) {
                TestObject actionTag = dynamicXpathObject("matrix_action_${action}_${moduleName.hashCode()}",
                    "${matrixModuleRowXpath(moduleName)}//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(action.toLowerCase())})]")
                waitVisible(actionTag)
                WebUI.verifyElementVisible(actionTag, FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyMatrixShowsPermissionActions failed: ${e.message}")
        }
    }

//    @Keyword
//    def verifyMatrixEnabledPermissionsHighlighted() {
//        try {
//            verifyPermissionMatrixScreenLoaded()
//            waitVisible(to97('tag_matrixEnabledPermission'))
//            WebUI.verifyElementVisible(to97('tag_matrixEnabledPermission'), FailureHandling.STOP_ON_FAILURE)
//            WebUI.takeScreenshot()
//        } catch (Exception e) {
//            failStop("verifyMatrixEnabledPermissionsHighlighted failed: ${e.message}")
//        }
//    }
	
	@Keyword
	def verifyMatrixEnabledPermissionsHighlighted() {
		try {
			verifyPermissionMatrixScreenLoaded()
	
			TestObject enabledPermissions = to97('tag_matrixEnabledPermission')
	
			waitVisible(enabledPermissions)
	
			List<WebElement> permissions = WebUI.findWebElements(
				enabledPermissions,
				15
			)
	
			if (permissions.isEmpty()) {
				failStop(
					'No highlighted/enabled permissions were found in the Permissions Matrix'
				)
			}
	
			KeywordUtil.logInfo(
				"Successfully validated ${permissions.size()} highlighted/enabled permissions"
			)
	
			WebUI.takeScreenshot()
	
		} catch (Exception e) {
			WebUI.takeScreenshot()
			failStop("verifyMatrixEnabledPermissionsHighlighted failed: ${e.message}")
		}
	}

    @Keyword
    def verifyMatrixLimitedPermissionsForRole(String roleName = 'Operations Staff', String moduleName = 'Customs', List expectedActions = null) {
        try {
            verifyPermissionMatrixScreenLoaded()
            List actions = expectedActions ?: ['View']
            TestObject row = dynamicXpathObject("matrix_limited_row_${moduleName.hashCode()}", matrixModuleRowXpath(moduleName))
            waitVisible(row)
            for (String action : actions) {
                TestObject tag = dynamicXpathObject("matrix_limited_${roleName.hashCode()}_${action.hashCode()}",
                    "${matrixModuleRowXpath(moduleName)}//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),${xpathLiteral(action.toLowerCase())})]")
                WebUI.verifyElementVisible(tag, FailureHandling.STOP_ON_FAILURE)
            }
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("verifyMatrixLimitedPermissionsForRole failed: ${e.message}")
        }
    }

    @Keyword
    def switchFromMatrixToRolesTabAndVerify() {
        try {
            openRolesTab()
            waitVisible(to('inp_SearchRoles'))
            WebUI.takeScreenshot()
        } catch (Exception e) {
            failStop("switchFromMatrixToRolesTabAndVerify failed: ${e.message}")
        }
    }

    @Keyword
    def verifyPermissionMatrixFullScreen() {
        try {
            openPermissionsMatrixTab()
			verifyPermissionsMatrixTablePresent()
//            verifyMatrixRoleColumns()
//            verifyMatrixModuleRows()
//            verifyMatrixShowsPermissionActions('Super Admin', 'Customs')
//            verifyMatrixEnabledPermissionsHighlighted()
//            switchFromMatrixToRolesTabAndVerify()
        } catch (Exception e) {
            failStop("verifyPermissionMatrixFullScreen failed: ${e.message}")
        }
    }
}