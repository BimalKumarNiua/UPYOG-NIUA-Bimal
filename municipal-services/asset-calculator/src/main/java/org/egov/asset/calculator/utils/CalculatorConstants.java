package org.egov.asset.calculator.utils;

public class CalculatorConstants {

	private CalculatorConstants() {
	}

	public static final String MDMS_EGF_MASTER = "egf-master";

	public static final String MDMS_FINANCIALYEAR = "FinancialYear";

	public static final String MDMS_FINACIALYEAR_PATH = "$.MdmsRes.egf-master.FinancialYear[?(@.code==\"{}\")]";

	public static final String MDMS_STARTDATE = "startingDate";

	public static final String MDMS_ENDDATE = "endingDate";

	public static final String MDMS_CALCULATIONTYPE_FINANCIALYEAR_PATH = "$.MdmsRes.BPA.CalculationType[?(@.financialYear=='{}')]";

	public static final String APPLICATION_FEE = "APPLICATION_FEE";

	public static final String MODULE_CODE = "FSM";

	public static final String FSM_JSONPATH_CODE = "$.MdmsRes.FSM";

	// mdms master names

	public static final String PROPERTY_TYPE = "PropertyType";
	public static final String FSM_CONFIG = "Config";
	public static final String FSM_ADVANCEPAYMENT = "AdvancePayment";
	public static final String FSM_CANCELLATIONFEE = "CancellationFee";

	// Error messages in FSM Calculator

	public static final String PARSING_ERROR = "PARSING ERROR";

	public static final String INVALID_PRICE = "INVALID PRICE";

	public static final String INVALID_MIN_ADVANCE_AMOUNT = "INVALID MIN ADVANCE AMOUNT";

	public static final String INVALID_MAX_ADVANCE_AMOUNT = "INVALID MAX ADVANCE AMOUNT";

	public static final String INVALID_CAPACITY = "INVALID CAPACITY";

	public static final String INVALID_UPDATE = "INVALID UPDATE";

	public static final String INVALID_ERROR = "INVALID ERROR";

	public static final String INVALID_APPLICATION_NUMBER = "INVALID APPLICATION NUMBER";

	public static final String CALCULATION_ERROR = "CALCULATION ERROR";

	public static final String APPLICATION_NOT_FOUND = "APPLICATION_NOT_FOUND";

	public static final String INVALID_BILLING_SLAB_ERROR = "INVALID_BILLING_SLAB_ERROR";

	public static final String INVALID_PROPERTY_TYPE = "INVALID_PROPERTY_TYPE";

	public static final String INVALID_TENANT_ID_MDMS_KEY = "INVALID TENANTID";

	public static final String INVALID_TENANT_ID_MDMS_MSG = "No data found for this tenentID";

	public static final String INVALID_SEARCH = "INVALID_SEARCH";

	public static final String EMPLOYEE_INVALID_SEARCH = "EMPLOYEE_INVALID_SEARCH";

	public static final String EMPLOYEE = "EMPLOYEE";

	public static final String FIXED_VALUE = "FIXEDVALUE";

	public static final String PERCENTAGE_VALUE = "PERCENTAGEVALUE";

	public static final String SUCCESS_MESSAGE = "Depreciation calculated successfully";
	public static final String CREATE = "CREATE";
	public static final String UPDATE = "UPDATE";
	public static final String SEARCH = "SEARCH";

	public static final String ZERO_PRICE_CHECK_STATUS = "zeroPricingStatus";
	public static final String CODE = "code";
	public static final String ACTIVE = "active";


}
