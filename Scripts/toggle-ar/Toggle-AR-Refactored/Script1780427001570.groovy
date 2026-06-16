import toggle_ar.ToggleArActions

def actions = new ToggleArActions()

actions.openLoginPage('https://dev-cc.aramex.com/login')
actions.loginWithMicrosoft('aditi.singh1@tothenew.com', '19430064')
actions.navigateToArManagement()
actions.searchArAccount('ACCOUNT_NAME', 'EXACT_MATCH', 'Sample AR RUH DTA', 'JED')
actions.openFirstSearchResult()
actions.toggleServiceChargeSettings()
actions.verifyServiceChargeSettingsPanelAndColumns()
actions.close()
