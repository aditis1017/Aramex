import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-92 | TC_U&RM_FUNC_025 | AC3 - Role already exists

def login = new MSLogin()
def roles = new RolesPermission()

String sourceRole = 'Super Admin'
if (binding.hasVariable('sourceRoleName') && sourceRoleName) {
	sourceRole = sourceRoleName as String
}

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Duplicate role and attempt to save with existing role name'
roles.openDuplicateRoleFormForRole(sourceRole)
roles.verifyDuplicateRoleFormPrefilledFromRole(sourceRole)
roles.setRoleNameEnglish(sourceRole)
roles.clickSaveRoleForm()
roles.verifyRoleAlreadyExistsMessage()
WebUI.takeScreenshot()
WebUI.closeBrowser()
