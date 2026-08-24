import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-92 | TC_U&RM_FUNC_024 | AC1/AC2 - Duplicate role and save

def login = new MSLogin()
def roles = new RolesPermission()

String sourceRole = 'Super Admin'
if (binding.hasVariable('sourceRoleName') && sourceRoleName) {
	sourceRole = sourceRoleName as String
}
String newRoleName = "AutomationDup-${System.currentTimeMillis()}"

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Open Duplicate form from Super Admin ellipsis menu'
roles.openDuplicateRoleFormForRole(sourceRole)
roles.verifyDuplicateRoleFormPrefilledFromRole(sourceRole)
'Modify duplicated role name and save'
roles.setRoleNameEnglish(newRoleName)
roles.clickSaveRoleForm()
roles.verifyDuplicateRoleSuccessMessage()
'Verify new role card appears on Roles tab'
roles.clearRoleSearch()
roles.verifyRoleVisible(newRoleName)
WebUI.takeScreenshot()
WebUI.closeBrowser()
