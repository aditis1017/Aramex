import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-96 | TC_U&RM_FUNC_021 | Invalid permission combination

def login = new MSLogin()
def roles = new RolesPermission()

String roleName = 'Super Admin'
String moduleName = 'Super Admin'
//if (binding.hasVariable('permRoleName') && permRoleName) {
//	roleName = permRoleName as String
//}
//if (binding.hasVariable('permModuleName') && permModuleName) {
//	moduleName = permModuleName as String
//}
'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
roles.openRoleDetailsAndEditPermissionsSuper(roleName)
roles.verifyEditPermissionsButtonDisabled() 
WebUI.takeScreenshot()
WebUI.closeBrowser()
