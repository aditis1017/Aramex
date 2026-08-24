import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-89 | TC_U&RM_FUNC_018 | AC7 - No roles found

def login = new MSLogin()
def roles = new RolesPermission()

String invalidSearch = 'zzzzinvalidrole999'

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Search for a non-existing role and verify No roles found message'
roles.searchRoles(invalidSearch)
roles.verifyNoRolesFoundMessage()
WebUI.takeScreenshot()
WebUI.closeBrowser()