package com.parking.pricing.internal;

import com.parking.pricing.IPricingPolicy;
import com.parking.pricing.PricingEstimateDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class PricingController {

    private final PricingRuleService ruleService;
    private final IPricingPolicy pricingPolicy;

    PricingController(PricingRuleService ruleService, IPricingPolicy pricingPolicy) {
        this.ruleService = ruleService;
        this.pricingPolicy = pricingPolicy;
    }

    @GetMapping("/pricing/rules")
    @PreAuthorize("hasRole('ADMIN')")
    List<PricingRule> listRules() {
        return ruleService.listRules();
    }

    @PostMapping("/pricing/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    PricingRule createRule(@RequestBody @Valid RuleRequest req) {
        return ruleService.createRule(req.zoneId(), req.spaceType(), req.ratePerHour(),
                req.validFrom(), req.validTo());
    }

    @PutMapping("/pricing/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    PricingRule updateRule(@PathVariable UUID id, @RequestBody @Valid RuleRequest req) {
        return ruleService.updateRule(id, req.zoneId(), req.spaceType(), req.ratePerHour(),
                req.validFrom(), req.validTo());
    }

    @DeleteMapping("/pricing/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
    }

    @GetMapping("/pricing/estimate")
    PricingEstimateDTO estimate(@RequestParam UUID spaceId, @RequestParam int durationMinutes) {
        return pricingPolicy.estimate(spaceId, durationMinutes);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    record RuleRequest(
            @NotNull UUID zoneId,
            @NotNull PricingRule.SpaceType spaceType,
            @NotNull @Positive BigDecimal ratePerHour,
            @NotNull LocalDateTime validFrom,
            LocalDateTime validTo
    ) {}
}
