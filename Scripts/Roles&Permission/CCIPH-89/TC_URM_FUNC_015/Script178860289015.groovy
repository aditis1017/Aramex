import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-89 | TC_U&RM_FUNC_015 | AC5 - Case insensitive search

def login = new MSLogin()
def roles = new RolesPermission()

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Enter lowercase admin and verify Super Admin role is displayed'
roles.searchRoles('admin')
roles.verifyOnlyRolesVisible(['Super Admin'])
WebUI.takeScreenshot()
WebUI.closeBrowser()