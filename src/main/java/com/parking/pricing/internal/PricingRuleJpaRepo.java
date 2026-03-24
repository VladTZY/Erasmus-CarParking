package com.parking.pricing.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PricingRuleJpaRepo extends JpaRepository<PricingRule, UUID> {}
