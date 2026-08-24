import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-92 | TC_U&RM_FUNC_026 | AC4 - Cancel duplicate without saving

def login = new MSLogin()
def roles = new RolesPermission()

String sourceRole = 'Super Admin'
if (binding.hasVariable('sourceRoleName') && sourceRoleName) {
	sourceRole = sourceRoleName as String
}
String unsavedRoleName = "AutomationDup-Cancel-${System.currentTimeMillis()}"

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Open Duplicate form, enter values, then close without saving'
roles.openDuplicateRoleFormForRole(sourceRole)
roles.verifyDuplicateRoleFormPrefilledFromRole(sourceRole)
roles.setRoleNameEnglish(unsavedRoleName)
roles.clickCloseRoleForm()
roles.verifyDuplicateRoleFormClosed()
'Verify unsaved duplicate role card does not appear'
roles.verifyRoleNotVisible(unsavedRoleName)
WebUI.takeScreenshot()
WebUI.closeBrowser()
