import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-91 | TC_U&RM_FUNC_019 | AC1/AC2 - Edit role and save

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
'Open Edit form from role card ellipsis menu'
roles.openEditRoleFormForRole(roleName)
roles.verifyEditRoleFormOpenWithPrefilledData(roleName)
String originalDescription = roles.getEditRoleDescriptionEnglish()
String updateSuffix = "AutoTest-${System.currentTimeMillis()}"
'Update role description and save'
//roles.setEditRoleDescriptionEnglish("${originalDescription} ${updateSuffix}".trim())
roles.setEditRoleDescriptionEnglish(" Super Admin ${updateSuffix}".trim())
roles.clickSaveEditRole()
roles.verifyEditRoleSuccessMessage()
roles.refreshRolesAndPermissionsPage()
roles.verifyRoleCardNameAndDescription(roleName, updateSuffix)
'Restore original description to avoid leaving test data'
roles.openEditRoleFormForRole(roleName)
roles.setEditRoleDescriptionEnglish(originalDescription)
roles.clickSaveEditRole()
roles.verifyEditRoleSuccessMessage()
roles.verifyRoleCardDescriptionEquals(roleName, originalDescription)
WebUI.takeScreenshot()
WebUI.closeBrowser()
