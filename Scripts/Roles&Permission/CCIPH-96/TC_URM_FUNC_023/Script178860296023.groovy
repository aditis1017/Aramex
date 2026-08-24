import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-96 | TC_U&RM_FUNC_023 | AC3/AC6 - Enable Delete permission and save

def login = new MSLogin()
def roles = new RolesPermission()

String roleName = 'Customs Manager'
String moduleName = 'Customs'
if (binding.hasVariable('permRoleName') && permRoleName) {
	roleName = permRoleName as String
}
if (binding.hasVariable('permModuleName') && permModuleName) {
	moduleName = permModuleName as String
}
'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
roles.openRoleDetailsAndEditPermissions(roleName)
roles.togglePermissionTag('Delete', moduleName)
roles.savePermissionsAndVerifySuccess()
roles.verifyPermissionTagEnabled('Delete', moduleName)
'Revert Delete permission to original disabled state'
roles.clickEditPermissionsOnRoleDetails()
roles.togglePermissionTag('Delete', moduleName)
roles.savePermissionsAndVerifySuccess()
WebUI.takeScreenshot()
WebUI.closeBrowser()
