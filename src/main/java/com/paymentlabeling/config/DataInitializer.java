package com.paymentlabeling.config;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.LabelingRule;
import com.paymentlabeling.repository.LabelRepository;
import com.paymentlabeling.repository.LabelingRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data initializer for seeding default labels and rules into the database.
 * This component runs on application startup and initializes the database
 * with predefined labels and labeling rules if none are available.
 *
 * Purpose: Ensure the system has basic labeling categories to start with.
 */
@Slf4j
@Component
public class DataInitializer {

    private final LabelRepository labelRepository;
    private final LabelingRuleRepository labelingRuleRepository;
    private final org.springframework.core.env.Environment env;

    public DataInitializer(LabelRepository labelRepository, LabelingRuleRepository labelingRuleRepository,
                          org.springframework.core.env.Environment env) {
        this.labelRepository = labelRepository;
        this.labelingRuleRepository = labelingRuleRepository;
        this.env = env;
    }

    /**
     * Initialize database with default labels and rules if none are available.
     * This method is called when the application is ready.
     * Skips initialization during testing (when test profile is active).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeData() {
        // Skip initialization if test profile is active
        String[] activeProfiles = env.getActiveProfiles();
        boolean isTestMode = java.util.Arrays.asList(activeProfiles).contains("test");
        
        if (isTestMode) {
            System.out.println("Test profile detected. Skipping data initialization.");
            return;
        }

        try {
            // Only initialize if no rules are available in the database
            if (labelingRuleRepository.count() == 0) {
                System.out.println("No labeling rules found. Initializing database with default labels and rules...");
                initializeDefaultLabels();
                initializeDefaultRules();
                System.out.println("Database initialization completed successfully.");
            } else {
                System.out.println("Labeling rules already exist. Skipping initialization.");
            }
        } catch (Exception e) {
            System.err.println("Error during data initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database with default data", e);
        }
    }

    /**
     * Initialize default labels into the database
     */
    private void initializeDefaultLabels() {
        List<Label> defaultLabels = List.of(
            createLabel("Grocery Shopping", "Purchases at grocery stores, supermarkets, and food markets"),
            createLabel("Restaurants & Cafes", "Restaurant, cafe, and food service purchases"),
            createLabel("Parking", "Parking fees, parking lot payments, and parking services"),
            createLabel("Transportation", "Public transportation, taxi, fuel, and vehicle-related expenses"),
            createLabel("Entertainment", "Cinema, theater, concert, sports, and other entertainment expenses"),
            createLabel("Utilities", "Water, electricity, gas, internet, and other utility bills"),
            createLabel("Clothing & Fashion", "Clothing stores, fashion, shoes, and accessories"),
            createLabel("Health & Pharmacy", "Pharmacy, medical supplies, and health-related purchases"),
            createLabel("Office & Stationery", "Office supplies, stationery, and work-related materials"),
            createLabel("Services", "Professional services, maintenance, and repairs")
        );

        labelRepository.saveAll(defaultLabels);
        System.out.println("Initialized " + defaultLabels.size() + " default labels");
    }

    private Label createLabel(String name, String description) {
        Label label = new Label();
        label.setName(name);
        label.setDescription(description);
        return label;
    }

    /**
     * Initialize default labeling rules into the database
     */
    private void initializeDefaultRules() {
        List<Label> labels = labelRepository.findAll();
        if (labels.isEmpty()) {
            System.out.println("No labels found. Cannot initialize rules without labels.");
            return;
        }

        // Create a map of label names for easier lookup
        java.util.Map<String, Label> labelMap = new java.util.HashMap<>();
        labels.forEach(label -> labelMap.put(label.getName(), label));

        List<LabelingRule> defaultRules = new java.util.ArrayList<>();

        // Grocery Shopping Rules
        defaultRules.add(createRule("Kaufland Grocery Store", "(?i).*KAUFLAND.*", labelMap.get("Grocery Shopping"), "Matches Kaufland grocery store transactions"));
        defaultRules.add(createRule("Tesco Grocery Store", "(?i).*TESCO.*", labelMap.get("Grocery Shopping"), "Matches Tesco grocery store transactions"));
        defaultRules.add(createRule("Billa Grocery Store", "(?i).*BILLA.*", labelMap.get("Grocery Shopping"), "Matches Billa grocery store transactions"));
        defaultRules.add(createRule("Carrefour Grocery Store", "(?i).*CARREFOUR.*", labelMap.get("Grocery Shopping"), "Matches Carrefour hypermarket transactions"));

        // Restaurants & Cafes Rules
        defaultRules.add(createRule("Restaurant and Cafe General Rule", "(?i).*(pizza|restaurant|cafe|burger|kebab|sushi|mcdonalds|kfc).*", labelMap.get("Restaurants & Cafes"), "Matches common restaurant, cafe, and food service names"));
        defaultRules.add(createRule("Fast Food Chain Rule", "(?i).*(mcdonald|subway|kfc|burger.*king).*", labelMap.get("Restaurants & Cafes"), "Matches fast food chain transactions"));

        // Parking Rules
        defaultRules.add(createRule("Hopin Parking Service", "(?i).*HOPIN.*PARKING.*", labelMap.get("Parking"), "Matches Hopin parking service transactions"));
        defaultRules.add(createRule("Parking Payment Rule", "(?i).*(parking|parkovanie|parkplatz).*", labelMap.get("Parking"), "Matches generic parking payment transactions"));

        // Transportation Rules
        defaultRules.add(createRule("Fuel and Gas Stations", "(?i).*(benzin|fuel|shell|mol|lukoil|aral|bp).*", labelMap.get("Transportation"), "Matches fuel and gas station transactions"));
        defaultRules.add(createRule("Public Transportation", "(?i).*(dpb|dpp|imhd|taxi|bus|tram|train|railway).*", labelMap.get("Transportation"), "Matches public transportation and taxi services"));
        defaultRules.add(createRule("Airlines and Flights", "(?i).*(airline|flight|ryanair|lufthansa|wizz|airfare).*", labelMap.get("Transportation"), "Matches airline and flight booking transactions"));

        // Entertainment Rules
        defaultRules.add(createRule("Cinema and Theater", "(?i).*(cinema|theater|theatre|kino|multiplex).*", labelMap.get("Entertainment"), "Matches cinema and theater ticket purchases"));
        defaultRules.add(createRule("Sports and Fitness", "(?i).*(gym|fitness|sport|yoga|swimming|pool).*", labelMap.get("Entertainment"), "Matches sports and fitness facility memberships"));
        defaultRules.add(createRule("Gaming and Streaming", "(?i).*(steam|playstation|xbox|netflix|spotify|youtube).*", labelMap.get("Entertainment"), "Matches gaming and streaming service subscriptions"));

        // Utilities Rules
        defaultRules.add(createRule("Electricity and Gas Bills", "(?i).*(elektrárne|gas|power|electricity|vuje).*", labelMap.get("Utilities"), "Matches electricity and gas utility bills"));
        defaultRules.add(createRule("Internet and Telecom", "(?i).*(internet|telecom|vodafone|orange|t-mobile|swisscom).*", labelMap.get("Utilities"), "Matches internet, mobile, and telecommunications bills"));
        defaultRules.add(createRule("Water Utility", "(?i).*(water|vodovod|waterworks).*", labelMap.get("Utilities"), "Matches water utility bills"));

        // Clothing & Fashion Rules
        defaultRules.add(createRule("Fast Fashion Retailers", "(?i).*(zara|h&m|inditex|uniqlo|gap|topshop).*", labelMap.get("Clothing & Fashion"), "Matches fast fashion retailer transactions"));
        defaultRules.add(createRule("Shoe and Footwear Stores", "(?i).*(shoe|footwear|sneaker|adidas|nike|puma|asics).*", labelMap.get("Clothing & Fashion"), "Matches shoe and footwear store transactions"));

        // Health & Pharmacy Rules
        defaultRules.add(createRule("Pharmacy", "(?i).*(pharmacy|lekáren|apoteka|drug.*store|medical.*supply).*", labelMap.get("Health & Pharmacy"), "Matches pharmacy and health supply transactions"));
        defaultRules.add(createRule("Healthcare Services", "(?i).*(hospital|clinic|doctor|dentist|dental|medical).*", labelMap.get("Health & Pharmacy"), "Matches healthcare provider transactions"));

        // Office & Stationery Rules
        defaultRules.add(createRule("Office Supply Stores", "(?i).*(office.*depot|staples|office.*max|papier.*shop).*", labelMap.get("Office & Stationery"), "Matches office and stationery store transactions"));

        // Services Rules
        defaultRules.add(createRule("Household and Repair Services", "(?i).*(repair|maintenance|plumber|electrician|contractor).*", labelMap.get("Services"), "Matches household repair and maintenance service transactions"));
        defaultRules.add(createRule("Professional Services", "(?i).*(lawyer|accountant|consultant|attorney|legal).*", labelMap.get("Services"), "Matches professional service provider transactions"));

        labelingRuleRepository.saveAll(defaultRules);
        System.out.println("Initialized " + defaultRules.size() + " default labeling rules");
    }

    private LabelingRule createRule(String name, String pattern, Label label, String description) {
        LabelingRule rule = new LabelingRule();
        rule.setName(name);
        rule.setRegexPattern(pattern);
        rule.setLabel(label);
        rule.setIsActive(true);
        rule.setDescription(description);
        return rule;
    }
}
