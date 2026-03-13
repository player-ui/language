import { flow } from "../../core/flow/index.js";
import {
  collection,
  input,
  action,
  text,
} from "../../__mocks__/generated/index.js";
import { binding as b } from "../../core/tagged-template/index.js";
import { expression as e } from "../../core/tagged-template/index.js";
import { template } from "../../core/template/index.js";
import type { Asset, Flow } from "@player-ui/types";

/**
 * Shared benchmark scenarios for comparing Kotlin and JavaScript DSL performance.
 * Each scenario must have an equivalent implementation in Kotlin to ensure fair comparison.
 */

/**
 * Simple form flow (~50 assets).
 *
 * Collection with 5 inputs (firstName, lastName, email, phone, address),
 * 2 actions (submit, cancel), navigation with 3 states, and data model.
 */
export const simpleFormFlow = (): Flow<Asset<"collection">> =>
  flow({
    id: "simple-form",
    views: [
      collection()
        .withId("form")
        .withLabel(text().withValue("User Form"))
        .withValues([
          input()
            .withBinding(b`user.firstName`)
            .withLabel(text().withValue("First Name"))
            .withPlaceholder("Enter first name"),
          input()
            .withBinding(b`user.lastName`)
            .withLabel(text().withValue("Last Name"))
            .withPlaceholder("Enter last name"),
          input()
            .withBinding(b`user.email`)
            .withLabel(text().withValue("Email"))
            .withPlaceholder("Enter email"),
          input()
            .withBinding(b`user.phone`)
            .withLabel(text().withValue("Phone"))
            .withPlaceholder("Enter phone"),
          input()
            .withBinding(b`user.address`)
            .withLabel(text().withValue("Address"))
            .withPlaceholder("Enter address"),
        ])
        .withActions([
          action()
            .withValue("submit")
            .withLabel(text().withValue("Submit"))
            .withMetaData({ role: "primary" }),
          action().withValue("cancel").withLabel(text().withValue("Cancel")),
        ]),
    ],
    navigation: {
      BEGIN: "FLOW_1",
      FLOW_1: {
        startState: "VIEW_form",
        VIEW_form: {
          state_type: "VIEW",
          ref: "form",
          transitions: {
            submit: "END_Done",
            cancel: "END_Cancelled",
          },
        },
        END_Done: { state_type: "END", outcome: "done" },
        END_Cancelled: { state_type: "END", outcome: "cancelled" },
      },
    },
    data: {
      user: {
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        address: "",
      },
    },
  });

/**
 * Template-heavy flow (~100 assets).
 *
 * Template iterating 20 product items, each with collection (simulating card structure)
 * and 2 actions. Tests template processing and nested structures.
 */
export const templateHeavyFlow = (): Flow<Asset<"collection">> =>
  flow({
    id: "template-heavy",
    views: [
      collection()
        .withId("products")
        .withLabel(text().withValue("Product List"))
        .template(
          template({
            data: b`products`,
            output: "values",
            dynamic: false,
            value: collection()
              .withLabel(text().withValue(b`products._index_.name`))
              .withValues([text().withValue(b`products._index_.price`)])
              .withActions([
                action()
                  .withValue("view")
                  .withLabel(text().withValue("View Details")),
                action()
                  .withValue("buy")
                  .withLabel(text().withValue("Buy Now")),
              ]),
          }),
        ),
    ],
    navigation: {
      BEGIN: "FLOW_1",
      FLOW_1: {
        startState: "VIEW_products",
        VIEW_products: {
          state_type: "VIEW",
          ref: "products",
          transitions: { "*": "END" },
        },
        END: { state_type: "END", outcome: "done" },
      },
    },
    data: {
      products: Array.from({ length: 20 }, (_, i) => ({
        name: `Product ${i}`,
        price: `$${(i + 1) * 10}.00`,
      })),
    },
  });

/**
 * Switch-heavy flow (~80 assets).
 *
 * Multiple views with static and dynamic switches for conditional rendering.
 * Tests switch performance and expression evaluation.
 */
export const switchHeavyFlow = (): Flow<Asset<"collection">> =>
  flow({
    id: "switch-heavy",
    views: [
      collection()
        .withId("locale-content")
        .switch(["label"], {
          isDynamic: false,
          cases: [
            {
              case: e`user.locale === 'es'`,
              asset: text().withValue("Español"),
            },
            {
              case: e`user.locale === 'fr'`,
              asset: text().withValue("Français"),
            },
            {
              case: e`user.locale === 'de'`,
              asset: text().withValue("Deutsch"),
            },
            {
              case: e`user.locale === 'ja'`,
              asset: text().withValue("日本語"),
            },
            { case: true, asset: text().withValue("English") },
          ],
        })
        .withValues([
          collection().switch(["label"], {
            isDynamic: true,
            cases: [
              {
                case: e`user.isPremium`,
                asset: text().withValue("Premium User"),
              },
              { case: true, asset: text().withValue("Standard User") },
            ],
          }),
          collection().switch(["label"], {
            isDynamic: false,
            cases: [
              { case: e`user.age >= 18`, asset: text().withValue("Adult") },
              { case: e`user.age >= 13`, asset: text().withValue("Teen") },
              { case: true, asset: text().withValue("Child") },
            ],
          }),
          collection().switch(["label"], {
            isDynamic: true,
            cases: [
              { case: e`user.verified`, asset: text().withValue("Verified") },
              { case: true, asset: text().withValue("Unverified") },
            ],
          }),
        ]),
    ],
    navigation: {
      BEGIN: "FLOW_1",
      FLOW_1: {
        startState: "VIEW_locale-content",
        "VIEW_locale-content": {
          state_type: "VIEW",
          ref: "locale-content",
          transitions: { "*": "END" },
        },
        END: { state_type: "END", outcome: "done" },
      },
    },
    data: {
      user: {
        locale: "en",
        isPremium: false,
        age: 25,
        verified: true,
      },
    },
  });

/**
 * Production-scale flow (~20k lines JSON).
 *
 * 5 views with deeply nested collections, templates, switches, and large data.
 * Mirrors a real production builder that generates ~20k lines of JSON.
 */
export const productionScaleFlow = (): Flow<Asset<"collection">> => {
  const groupNames = [
    "Personal",
    "Contact",
    "Address",
    "Employment",
    "Education",
    "Financial",
    "Medical",
    "Preferences",
  ];
  const fieldNames = ["field1", "field2", "field3", "field4", "field5"];
  const categoryNames = [
    "profile",
    "security",
    "notifications",
    "privacy",
    "billing",
    "integrations",
  ];
  const settingNames = [
    "setting1",
    "setting2",
    "setting3",
    "setting4",
    "setting5",
    "setting6",
    "setting7",
    "setting8",
  ];
  const sectionNames = [
    "analytics",
    "reports",
    "alerts",
    "activity",
    "performance",
    "inventory",
    "marketing",
    "support",
    "compliance",
    "feedback",
  ];

  const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);

  return flow({
    id: "production-scale",
    views: [
      // View 1: Large registration form (8 groups x 5 inputs = 40 inputs)
      collection()
        .withId("registration")
        .withLabel(text().withValue("Complete Registration"))
        .withValues(
          groupNames.map((groupName) =>
            collection()
              .withLabel(text().withValue(`${groupName} Information`))
              .withValues(
                fieldNames.map((field) =>
                  input()
                    .withBinding(b`registration.${groupName}.${field}`)
                    .withLabel(
                      text().withValue(`${groupName} ${capitalize(field)}`),
                    )
                    .withPlaceholder(`Enter ${groupName} ${field}`),
                ),
              ),
          ),
        )
        .withActions([
          action()
            .withValue("next")
            .withLabel(text().withValue("Continue"))
            .withMetaData({ role: "primary" }),
          action().withValue("save").withLabel(text().withValue("Save Draft")),
          action().withValue("cancel").withLabel(text().withValue("Cancel")),
        ]),
      // View 2: Product catalog (template over 50 products)
      collection()
        .withId("catalog")
        .withLabel(text().withValue("Product Catalog"))
        .template(
          template({
            data: b`products`,
            output: "values",
            dynamic: false,
            value: collection()
              .withLabel(text().withValue(b`products._index_.name`))
              .withValues([
                text().withValue(b`products._index_.description`),
                text().withValue(b`products._index_.price`),
                text().withValue(b`products._index_.sku`),
              ])
              .withActions([
                action()
                  .withValue("view")
                  .withLabel(text().withValue("View Details")),
                action()
                  .withValue("add-to-cart")
                  .withLabel(text().withValue("Add to Cart"))
                  .withMetaData({ role: "primary" }),
                action()
                  .withValue("wishlist")
                  .withLabel(text().withValue("Add to Wishlist")),
              ]),
          }),
        ),
      // View 3: Order history (30 orders x 5 line items each)
      collection()
        .withId("orders")
        .withLabel(text().withValue("Order History"))
        .template(
          template({
            data: b`orders`,
            output: "values",
            dynamic: false,
            value: collection()
              .withLabel(text().withValue(b`orders._index_.orderNumber`))
              .withValues([
                text().withValue(b`orders._index_.status`),
                text().withValue(b`orders._index_.total`),
                text().withValue(b`orders._index_.date`),
              ])
              .template(
                template({
                  data: b`orders._index_.items`,
                  output: "values",
                  dynamic: false,
                  value: collection()
                    .withLabel(
                      text().withValue(b`orders._index_.items._index1_.name`),
                    )
                    .withValues([
                      text().withValue(
                        b`orders._index_.items._index1_.quantity`,
                      ),
                      text().withValue(b`orders._index_.items._index1_.price`),
                    ]),
                }),
              ),
          }),
        ),
      // View 4: Dashboard (10 switch blocks x 4 cases + templates)
      collection()
        .withId("dashboard")
        .switch(["label"], {
          isDynamic: true,
          cases: [
            {
              case: e`user.isPremium`,
              asset: text().withValue("Premium Dashboard"),
            },
            { case: true, asset: text().withValue("Standard Dashboard") },
          ],
        })
        .withValues(
          sectionNames.map((sectionName) =>
            collection()
              .switch(["label"], {
                isDynamic: false,
                cases: [
                  {
                    case: e`user.locale === 'es'`,
                    asset: text().withValue(`${capitalize(sectionName)} (ES)`),
                  },
                  {
                    case: e`user.locale === 'fr'`,
                    asset: text().withValue(`${capitalize(sectionName)} (FR)`),
                  },
                  {
                    case: e`user.locale === 'de'`,
                    asset: text().withValue(`${capitalize(sectionName)} (DE)`),
                  },
                  {
                    case: true,
                    asset: text().withValue(capitalize(sectionName)),
                  },
                ],
              })
              .template(
                template({
                  data: b`dashboard.${sectionName}`,
                  output: "values",
                  dynamic: true,
                  value: text().withValue(
                    b`dashboard.${sectionName}._index_.value`,
                  ),
                }),
              ),
          ),
        ),
      // View 5: Settings (6 categories x 8 inputs = 48 inputs)
      collection()
        .withId("settings")
        .withLabel(text().withValue("Account Settings"))
        .withValues(
          categoryNames.map((category) =>
            collection()
              .withLabel(text().withValue(`${capitalize(category)} Settings`))
              .withValues(
                settingNames.map((setting) =>
                  input()
                    .withBinding(b`settings.${category}.${setting}`)
                    .withLabel(
                      text().withValue(
                        `${capitalize(category)} ${capitalize(setting)}`,
                      ),
                    ),
                ),
              )
              .withActions([
                action()
                  .withValue(`save-${category}`)
                  .withLabel(text().withValue(`Save ${capitalize(category)}`))
                  .withMetaData({ role: "primary" }),
                action()
                  .withValue(`reset-${category}`)
                  .withLabel(text().withValue(`Reset ${capitalize(category)}`)),
              ]),
          ),
        ),
    ],
    navigation: {
      BEGIN: "FLOW_1",
      FLOW_1: {
        startState: "VIEW_registration",
        VIEW_registration: {
          state_type: "VIEW",
          ref: "registration",
          transitions: { next: "VIEW_catalog", cancel: "END_Cancelled" },
        },
        VIEW_catalog: {
          state_type: "VIEW",
          ref: "catalog",
          transitions: { next: "VIEW_orders", back: "VIEW_registration" },
        },
        VIEW_orders: {
          state_type: "VIEW",
          ref: "orders",
          transitions: { next: "VIEW_dashboard", back: "VIEW_catalog" },
        },
        VIEW_dashboard: {
          state_type: "VIEW",
          ref: "dashboard",
          transitions: { next: "VIEW_settings", back: "VIEW_orders" },
        },
        VIEW_settings: {
          state_type: "VIEW",
          ref: "settings",
          transitions: { done: "END_Done", back: "VIEW_dashboard" },
        },
        END_Done: { state_type: "END", outcome: "done" },
        END_Cancelled: { state_type: "END", outcome: "cancelled" },
      },
    },
    data: {
      registration: Object.fromEntries(
        groupNames.map((group) => [
          group,
          Object.fromEntries(
            Array.from({ length: 5 }, (_, i) => [`field${i + 1}`, ""]),
          ),
        ]),
      ),
      products: Array.from({ length: 50 }, (_, i) => ({
        name: `Product ${i}`,
        description: `Description for product ${i} with detailed information`,
        price: `$${(i + 1) * 10}.00`,
        sku: `SKU-${1000 + i}`,
      })),
      orders: Array.from({ length: 30 }, (_, i) => ({
        orderNumber: `ORD-${2000 + i}`,
        status:
          i % 3 === 0 ? "delivered" : i % 3 === 1 ? "shipped" : "processing",
        total: `$${(i + 1) * 50}.00`,
        date: `2026-${String((i % 12) + 1).padStart(2, "0")}-${String((i % 28) + 1).padStart(2, "0")}`,
        items: Array.from({ length: 5 }, (_, j) => ({
          name: `Item ${j} of Order ${i}`,
          quantity: `${j + 1}`,
          price: `$${(j + 1) * 15}.00`,
        })),
      })),
      dashboard: Object.fromEntries(
        sectionNames.map((section) => [
          section,
          Array.from({ length: 8 }, (_, i) => ({
            value: `${section} metric ${i}`,
          })),
        ]),
      ),
      settings: Object.fromEntries(
        categoryNames.map((category) => [
          category,
          Object.fromEntries(
            Array.from({ length: 8 }, (_, i) => [`setting${i + 1}`, ""]),
          ),
        ]),
      ),
      user: {
        isPremium: true,
        locale: "en",
      },
    },
  });
};

/**
 * Complex nested flow (~200 assets, production-like).
 *
 * 3 views with deeply nested collections, templates, and switches.
 * Represents realistic production content with multiple features combined.
 */
export const complexNestedFlow = (): Flow<Asset<"collection">> =>
  flow({
    id: "complex-nested",
    views: [
      // View 1: Nested collections with inputs
      collection()
        .withId("registration")
        .withLabel(text().withValue("Registration Form"))
        .withValues([
          collection()
            .withLabel(text().withValue("Personal Information"))
            .withValues([
              input()
                .withBinding(b`user.firstName`)
                .withLabel(text().withValue("First Name")),
              input()
                .withBinding(b`user.lastName`)
                .withLabel(text().withValue("Last Name")),
              input()
                .withBinding(b`user.email`)
                .withLabel(text().withValue("Email")),
            ]),
          collection()
            .withLabel(text().withValue("Address"))
            .withValues([
              input()
                .withBinding(b`user.address.street`)
                .withLabel(text().withValue("Street")),
              input()
                .withBinding(b`user.address.city`)
                .withLabel(text().withValue("City")),
              input()
                .withBinding(b`user.address.state`)
                .withLabel(text().withValue("State")),
              input()
                .withBinding(b`user.address.zip`)
                .withLabel(text().withValue("ZIP Code")),
            ]),
          collection()
            .withLabel(text().withValue("Preferences"))
            .withValues([
              input()
                .withBinding(b`user.preferences.newsletter`)
                .withLabel(text().withValue("Newsletter")),
              input()
                .withBinding(b`user.preferences.notifications`)
                .withLabel(text().withValue("Notifications")),
            ]),
        ]),
      // View 2: Template with nested structures
      collection()
        .withId("orders")
        .withLabel(text().withValue("Your Orders"))
        .template(
          template({
            data: b`orders`,
            output: "values",
            dynamic: false,
            value: collection()
              .withLabel(text().withValue(b`orders._index_.orderNumber`))
              .withValues([
                text().withValue(b`orders._index_.product`),
                text().withValue(b`orders._index_.total`),
              ])
              .template(
                template({
                  data: b`orders._index_.items`,
                  output: "values",
                  dynamic: false,
                  value: text().withValue(
                    b`orders._index_.items._index1_.name`,
                  ),
                }),
              ),
          }),
        ),
      // View 3: Combination of switches and templates
      collection()
        .withId("dashboard")
        .switch(["label"], {
          isDynamic: true,
          cases: [
            {
              case: e`user.isPremium`,
              asset: text().withValue("Premium Dashboard"),
            },
            { case: true, asset: text().withValue("Standard Dashboard") },
          ],
        })
        .withValues([
          collection()
            .switch(["label"], {
              isDynamic: false,
              cases: [
                {
                  case: e`user.locale === 'es'`,
                  asset: text().withValue("Notificaciones"),
                },
                { case: true, asset: text().withValue("Notifications") },
              ],
            })
            .template(
              template({
                data: b`notifications`,
                output: "values",
                dynamic: true,
                value: text().withValue(b`notifications._index_.message`),
              }),
            ),
        ]),
    ],
    navigation: {
      BEGIN: "FLOW_1",
      FLOW_1: {
        startState: "VIEW_registration",
        VIEW_registration: {
          state_type: "VIEW",
          ref: "registration",
          transitions: { next: "VIEW_orders" },
        },
        VIEW_orders: {
          state_type: "VIEW",
          ref: "orders",
          transitions: { next: "VIEW_dashboard" },
        },
        VIEW_dashboard: {
          state_type: "VIEW",
          ref: "dashboard",
          transitions: { "*": "END" },
        },
        END: { state_type: "END", outcome: "done" },
      },
    },
    data: {
      user: {
        firstName: "",
        lastName: "",
        email: "",
        address: {
          street: "",
          city: "",
          state: "",
          zip: "",
        },
        preferences: {
          newsletter: false,
          notifications: true,
        },
        isPremium: true,
        locale: "en",
      },
      orders: Array.from({ length: 15 }, (_, i) => ({
        orderNumber: `ORD-${1000 + i}`,
        product: `Product ${i}`,
        total: `$${(i + 1) * 25}.00`,
        items: [{ name: "Item 1" }, { name: "Item 2" }, { name: "Item 3" }],
      })),
      notifications: Array.from({ length: 10 }, (_, i) => ({
        message: `Notification ${i}`,
      })),
    },
  });
