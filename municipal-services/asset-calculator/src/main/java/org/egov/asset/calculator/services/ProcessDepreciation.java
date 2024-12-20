package org.egov.asset.calculator.services;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.asset.calculator.config.CalculatorConfig;
import org.egov.asset.calculator.repository.CustomDepreciationRepository;
import org.egov.asset.calculator.web.models.DepreciationDetail;
import org.egov.asset.calculator.repository.DepreciationDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
@AllArgsConstructor
@Slf4j
public class ProcessDepreciation {

    private  DepreciationDetailRepository depreciationDetailRepository;
    private final DepreciationDetailRepository depreciationDetailsRepository;
    private final CustomDepreciationRepository customDepreciationRepository;

    @Autowired
    private CalculatorConfig config;

    @Autowired
    public ProcessDepreciation(DepreciationDetailRepository depreciationDetailsRepository, CustomDepreciationRepository customDepreciationRepository) {
        this.depreciationDetailsRepository = depreciationDetailsRepository;
        this.customDepreciationRepository = customDepreciationRepository;
    }


    /**
     * Executes the bulk depreciation calculation at DB level.
     *
     * @param tenantId The tenant ID.
     * @return Success message.
     */
    public void executeBulkDepreciationProcedure(String tenantId) {
        try {
            customDepreciationRepository.executeBulkDepreciationProcedure(tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Error executing bulk depreciation procedure for tenant: " + tenantId, e);
        }
    }

    /**
     * Executes the single and legacy date depreciation calculation at DB level.
     *
     * @param tenantId The tenant ID.
     * @param assetId  The ID of the asset.
     * @return Success message.
     */
    public String executeSingleAndLegacyDepreciationProcedure(String tenantId, String assetId) {
        try {
            customDepreciationRepository.executeSingleAndLegacyDateDepreciationCalProcedure(tenantId, assetId);
            return "Single and legacy depreciation procedure executed successfully for tenant: " + tenantId + ", asset: " + assetId;
        } catch (Exception e) {
            throw new RuntimeException("Error executing single and legacy depreciation procedure for tenant: " + tenantId + ", asset: " + assetId, e);
        }
    }

    // Code level calculations
    public void calculateDepreciationForSingleAsset(Long assetId) {
        Map<String, Object> asset = null;

        if (!(Boolean) asset.get("isDepreciationApplicable") || !(Boolean) asset.get("isActive")) {
            return; // Skip non-eligible assets
        }

        double bookValue = (Double) asset.get("bookValue");
        double purchaseValue = (Double) asset.get("purchaseValue");
        int assetLife = (Integer) asset.get("assetLife");
        LocalDate purchaseDate = LocalDate.parse((String) asset.get("purchaseDate"));
        double minValue = (Double) asset.get("minimumValue");
        double depreciationRate = 100.0 / assetLife;

        // Remaining life calculation
        long elapsedYears = ChronoUnit.YEARS.between(purchaseDate, LocalDate.now());
        int remainingLife = assetLife - (int) elapsedYears;

        if (remainingLife <= 0 || bookValue <= minValue) return;

        double depreciationValue = Math.min((bookValue * depreciationRate / 100), bookValue - minValue);

        // Upsert logic
        LocalDate fromDate = LocalDate.now().withDayOfYear(1);
        LocalDate toDate = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
        DepreciationDetail detail = depreciationDetailRepository
                .findByAssetIdAndFromDate(assetId, fromDate)
                .orElse(new DepreciationDetail());

        detail.setAssetId(assetId);
        detail.setFromDate(fromDate);
        detail.setToDate(toDate);
        detail.setDepreciationValue(depreciationValue);
        detail.setBookValue(bookValue - depreciationValue);
        detail.setUpdatedAt(LocalDate.now());

        depreciationDetailRepository.save(detail);
    }

//    public void calculateBulkDepreciation() {
//        List<Map<String, Object>> assets = fetchEligibleAssets();
//        assets.forEach(asset -> calculateDepreciationForSingleAsset((Long) asset.get("id")));
//    }

//    private Map<String, Object> fetchAssetDetails(Long assetId) {
//        return restTemplate.getForObject(assetServiceUrl + "/api/assets/" + assetId, Map.class);
//    }
//
//    private List<Map<String, Object>> fetchEligibleAssets() {
//        return restTemplate.getForObject(assetServiceUrl + "/api/assets/eligible", List.class);
//    }

}
