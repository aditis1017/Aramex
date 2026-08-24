import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-91 | TC_U&RM_FUNC_020 | AC4 - Cancel edit without saving

def login = new MSLogin()
def roles = new RolesPermission()

String roleName = 'Super Admin'
if (binding.hasVariable('editRoleName') && editRoleName) {
	roleName = editRoleName as String
}

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
String originalDescription = roles.getRoleCardDescriptionText(roleName)
String unsavedDescription = "Should-Not-Save-${System.currentTimeMillis()}"
'Open Edit form, change fields, then cancel without saving'
roles.openEditRoleFormForRole(roleName)
roles.verifyEditRoleFormOpenWithPrefilledData(roleName)
roles.setEditRoleDescriptionEnglish(unsavedDescription)
roles.clickCloseEditRole()
roles.verifyEditRoleFormClosed()
'Verify role card still shows original description (no update applied)'
roles.verifyRoleCardDescriptionEquals(roleName, originalDescription)
WebUI.takeScreenshot()
WebUI.closeBrowser()
