package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Route(value = "boiler-room", layout = MainLayout.class)
@CssImport("./views/boiler-room-view.css")
@PageTitle("Котельня | Home Heating")
public class BoilerRoomView extends VerticalLayout {

    private final Div schematic = new Div();

    private Registration pollRegistration;

    private static final String DEVICE_ID = "BOILER_ROOM_01";

    private final TelemetryService telemetryService;

    // Значення не вважаються відомими, доки контролер їх не передасть.
    private double boilerSupply = Double.NaN;
    private double boilerReturn = Double.NaN;
    private double flueTemperature = Double.NaN;

    // П'ять зон теплоакумулятора: accumulator_01 ... accumulator_05.
    private double accumulatorTop = Double.NaN;
    private double accumulatorZoneTop = Double.NaN;
    private double accumulatorMiddle = Double.NaN;
    private double accumulatorZoneBottom = Double.NaN;
    private double accumulatorBottom = Double.NaN;

    private double radiatorIn = Double.NaN;
    private double radiatorOut = Double.NaN;
    private double floorIn = Double.NaN;
    private double floorOut = Double.NaN;
    private double dhwSupply = Double.NaN;
    private double dhwTemperature = Double.NaN;
    private double systemPressure = Double.NaN;
    private double electricBoilerSupply = Double.NaN;
    private double electricBoilerReturn = Double.NaN;

    // Ці стани не передаються поточним контролером — не виводимо їх як виміряні.
    private boolean radiatorHeatRequest;
    private boolean floorHeatRequest;
    private boolean dhwHeatRequest;
    private boolean electricBoilerOn;
    private boolean electricBoilerHeatRequest;

    private boolean boilerPumpOn;
    private boolean radiatorPumpOn;
    private boolean floorPumpOn;
    private boolean dhwPumpOn;
    private boolean electricBoilerPumpOn;

    private double accumulatorChargePercent = Double.NaN;
    private Instant lastTelemetryAt;

    private Span statusTitle;
    private Span statusDescription;

    // Елементи інтерфейсу
    private final Map<String, Span> temperatureValues =
            new LinkedHashMap<>();

    private final Map<String, Span> pumpStates =
            new LinkedHashMap<>();

    private final Map<String, Span> systemValues =
            new LinkedHashMap<>();

    public BoilerRoomView(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;

        addClassName("boiler-room-view");

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(
                buildHeader(),
                buildMainContent(),
                buildFooter()
        );
    }

    // =========================================================
    // HEADER
    // =========================================================

    private Div buildHeader() {
        Div header = new Div();
        header.addClassName("br-header");

        Div titleGroup = new Div();
        titleGroup.addClassName("br-title-group");

        H1 title = new H1("Котельня");
        title.addClassName("br-title");

        Paragraph subtitle = new Paragraph(
                "Система опалення"
        );
        subtitle.addClassName("br-subtitle");

        titleGroup.add(title, subtitle);

        Div status = new Div();
        status.addClassName("br-system-status");

        Span dot = new Span();
        dot.addClassName("br-status-dot");

        Div statusText = new Div();
        statusText.addClassName("br-status-copy");

        statusTitle = new Span("ОЧІКУВАННЯ ДАНИХ");
        statusTitle.addClassName("br-status-title");

        statusDescription = new Span("Очікування телеметрії від контролера");
        statusDescription.addClassName("br-status-description");

        statusText.add(statusTitle, statusDescription);
        status.add(dot, statusText);

        header.add(titleGroup, status);

        return header;
    }

    // =========================================================
    // MAIN CONTENT
    // =========================================================

    private Div buildMainContent() {
        Div main = new Div();
        main.addClassName("br-main-grid");

        // Картка зі схемою
        Div schematicCard = new Div();
        schematicCard.addClassName("br-card");
        schematicCard.addClassName("br-schematic-card");

        Div cardHeading = new Div();
        cardHeading.addClassName("br-card-heading");

        Div headingText = new Div();

        H1 schemeTitle = new H1("Гідравлічна схема");
        schemeTitle.addClassName("br-section-title");

        Paragraph schemeDescription = new Paragraph(
                "Твердопаливний котел · ТА · опалювальні контури"
        );
        schemeDescription.addClassName("br-section-description");

        headingText.add(schemeTitle, schemeDescription);

        Div legend = new Div();
        legend.addClassName("br-legend");

        legend.add(legendItem("br-legend-hot", "Подача"));
        legend.add(legendItem("br-legend-cold", "Обратка"));

        cardHeading.add(headingText, legend);

        schematic.addClassName("br-schematic");

        schematicCard.add(cardHeading, schematic);

        // Бічна панель
        Div sidePanel = new Div();
        sidePanel.addClassName("br-side-panel");

        sidePanel.add(
                buildTemperaturesCard(),
                buildPumpsCard(),
                buildSystemCard()
        );

        main.add(schematicCard, sidePanel);

        return main;
    }

    private Div legendItem(String colorClass, String text) {
        Div item = new Div();
        item.addClassName("br-legend-item");

        Span line = new Span();
        line.addClassName("br-legend-line");
        line.addClassName(colorClass);

        Span label = new Span(text);

        item.add(line, label);

        return item;
    }

    // =========================================================
    // TEMPERATURES CARD
    // =========================================================

    private Div buildTemperaturesCard() {
        Div card = new Div();
        card.addClassName("br-card");
        card.addClassName("br-info-card");

        card.add(sectionHeading(
                "Температури",
                "Поточні показники контролера"
        ));

        Div rows = new Div();
        rows.addClassName("br-reading-list");

        rows.add(readingRow(
                "Котел ТП · подача",
                "boilerSupply",
                "°C"
        ));

        rows.add(readingRow(
                "Котел ТП · зворотка",
                "boilerReturn",
                "°C"
        ));

        rows.add(readingRow(
                "Димові гази",
                "flueTemperature",
                "°C"
        ));

        rows.add(readingRow(
                "ТА · верх",
                "accumulatorTop",
                "°C"
        ));

        rows.add(readingRow(
                "ТА · середина",
                "accumulatorMiddle",
                "°C"
        ));

        rows.add(readingRow(
                "ТА · низ",
                "accumulatorBottom",
                "°C"
        ));

        rows.add(readingRow(
                "Радіатори · подача",
                "radiatorIn",
                "°C"
        ));

        rows.add(readingRow(
                "Радіатори · зворотка",
                "radiatorOut",
                "°C"
        ));
        rows.add(readingRow(
                "Підлога · подача",
                "floorIn",
                "°C"
        ));
        rows.add(readingRow(
                "Підлога · зворотка",
                "floorOut",
                "°C"
        ));
        rows.add(readingRow(
                "Бойлер · подача",
                "dhwSupply",
                "°C"
        ));
        rows.add(readingRow(
                "Бойлер · зворотка",
                "dhwTemperature",
                "°C"
        ));

        rows.add(readingRow(
                "Електрокотел · подача",
                "electricBoilerSupply",
                "°C"
        ));
        rows.add(readingRow(
                "Електрокотел · зворотка",
                "electricBoilerReturn",
                "°C"
        ));

        card.add(rows);

        return card;
    }

    // =========================================================
    // PUMPS CARD
    // =========================================================

    private Div buildPumpsCard() {
        Div card = new Div();
        card.addClassName("br-card");
        card.addClassName("br-info-card");

        card.add(sectionHeading(
                "Насоси",
                "Стан виконавчих механізмів"
        ));

        Div rows = new Div();
        rows.addClassName("br-pump-list");

        rows.add(pumpRow("Котловий контур", "boilerPump"));
        rows.add(pumpRow("Радіатори", "radiatorPump"));
        rows.add(pumpRow("Тепла підлога", "floorPump"));
        rows.add(pumpRow("Бойлер ГВП", "dhwPump"));
        rows.add(pumpRow("Електро котел", "electricBoilerPump"));

        card.add(rows);

        return card;
    }

    // =========================================================
    // SYSTEM CARD
    // =========================================================

    private Div buildSystemCard() {
        Div card = new Div();
        card.addClassName("br-card");
        card.addClassName("br-info-card");

        card.add(sectionHeading(
                "Стан системи",
                "Загальна інформація"
        ));

        Div rows = new Div();
        rows.addClassName("br-system-list");

        rows.add(systemRow(
                "Тиск системи",
                "pressure",
                "бар"
        ));

        rows.add(systemRow(
                "Теплоакумулятор",
                "accumulatorVolume",
                "л"
        ));

        rows.add(systemRow(
                "Останнє оновлення",
                "lastUpdate",
                ""
        ));

        card.add(rows);

        return card;
    }

    private Div sectionHeading(
            String title,
            String description
    ) {
        Div heading = new Div();
        heading.addClassName("br-info-heading");

        Span titleSpan = new Span(title);
        titleSpan.addClassName("br-info-title");

        Span descriptionSpan = new Span(description);
        descriptionSpan.addClassName("br-info-description");

        heading.add(titleSpan, descriptionSpan);

        return heading;
    }

    // =========================================================
    // TEMPERATURE ROW
    // =========================================================

    private Div readingRow(
            String label,
            String key,
            String unit
    ) {
        Div row = new Div();
        row.addClassName("br-reading-row");

        Span name = new Span(label);
        name.addClassName("br-reading-name");

        Div number = new Div();
        number.addClassName("br-reading-number");

        Span valueSpan = new Span();
        valueSpan.addClassName("br-reading-value");

        Span unitSpan = new Span(unit);
        unitSpan.addClassName("br-reading-unit");

        number.add(valueSpan, unitSpan);

        row.add(name, number);

        temperatureValues.put(key, valueSpan);

        return row;
    }

    // =========================================================
    // PUMP ROW
    // =========================================================

    private Div pumpRow(String name, String key) {
        Div row = new Div();
        row.addClassName("br-pump-row");

        Span icon = new Span();
        icon.addClassName("br-pump-indicator");

        Span label = new Span(name);
        label.addClassName("br-pump-name");

        Span state = new Span("—");
        state.addClassName("br-pump-state");

        row.add(icon, label, state);

        pumpStates.put(key, state);

        return row;
    }

    // =========================================================
    // SYSTEM ROW
    // =========================================================

    private Div systemRow(
            String label,
            String key,
            String unit
    ) {
        Div row = new Div();
        row.addClassName("br-system-row");

        Span name = new Span(label);
        name.addClassName("br-system-label");

        Span result = new Span();
        result.addClassName("br-system-value");

        if ("pressure".equals(key)) {
            result.setText("—");
        } else if ("accumulatorVolume".equals(key)) {
            result.setText("1000 " + unit);
        } else {
            result.setText("—");
        }

        row.add(name, result);

        systemValues.put(key, result);

        return row;
    }

    // =========================================================
    // FOOTER
    // =========================================================

    private Div buildFooter() {
        Div footer = new Div();
        footer.addClassName("br-footer");

        Div footerHeading = new Div();
        footerHeading.addClassName("br-footer-heading");

        Span title = new Span("Події та попередження");
        title.addClassName("br-footer-title");

        Span badge = new Span("1 повідомлення");
        badge.addClassName("br-event-badge");

        footerHeading.add(title, badge);

        Div event = new Div();
        event.addClassName("br-event");

        Span eventIcon = new Span("i");
        eventIcon.addClassName("br-event-icon");

        Div eventText = new Div();
        eventText.addClassName("br-event-text");

        Span eventTitle = new Span("Очікування телеметрії");
        eventTitle.addClassName("br-event-title");

        Span eventDescription = new Span(
                "Інтерфейс готовий. Після реєстрації пристрою " +
                        "тут з’являться актуальні дані."
        );
        eventDescription.addClassName("br-event-description");

        eventText.add(eventTitle, eventDescription);
        event.add(eventIcon, eventText);

        footer.add(footerHeading, event);

        return footer;
    }

    // =========================================================
    // SVG SCHEMATIC
    // =========================================================

    private void renderSchematic() {
        String svg = """
                        <svg class="br-svg" viewBox="0 0 1100 650" role="img"
                             aria-label="Схема котельні з котлом, теплоакумулятором та контурами опалення"
                             xmlns="http://www.w3.org/2000/svg">
                
                          <defs>
                            <pattern id="br-grid" width="32" height="32"
                                     patternUnits="userSpaceOnUse">
                              <path d="M 32 0 L 0 0 0 32"
                                    fill="none" stroke="#26364a" stroke-width="1"/>
                            </pattern>
                
                            <linearGradient id="br-tank" x1="0" x2="1">
                              <stop offset="0" stop-color="#26384d"/>
                              <stop offset=".22" stop-color="#18283a"/>
                              <stop offset=".78" stop-color="#18283a"/>
                              <stop offset="1" stop-color="#31465b"/>
                            </linearGradient>
                
                            <linearGradient id="br-boiler" x1="0" x2="1">
                              <stop offset="0" stop-color="#23364b"/>
                              <stop offset="1" stop-color="#142236"/>
                            </linearGradient>
                
                            <filter id="br-shadow" x="-20%" y="-20%"
                                    width="140%" height="140%">
                              <feDropShadow dx="0" dy="8" stdDeviation="8"
                                            flood-color="#020817" flood-opacity=".32"/>
                            </filter>
                          </defs>
                
                          <rect width="1100" height="650" rx="18" fill="#0b1422"/>
                          <rect width="1100" height="650" rx="18"
                                fill="url(#br-grid)" opacity=".72"/>
                
                          <!-- Main hot supply and return trunks -->
                          <g fill="none" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M205 220 H310 V150 H430"
                                  stroke="#ff684f" stroke-width="12"/>
                            <path d="M430 150 H475"
                                  stroke="#ff684f" stroke-width="12"/>
                            <path d="M590 150 H675 V120 H760"
                                  stroke="#ff684f" stroke-width="12"/>
                            <path d="M675 150 V300 H760"
                                  stroke="#ff684f" stroke-width="12"/>
                            <path d="M675 300 V485 H760"
                                  stroke="#ff684f" stroke-width="12"/>
                
                            <path d="M760 180 H710 V565 H340 V470 H205"
                                  stroke="#36b8ed" stroke-width="12"/>
                            <path d="M760 360 H710"
                                  stroke="#36b8ed" stroke-width="12"/>
                            <path d="M760 525 H710"
                                  stroke="#36b8ed" stroke-width="12"/>
                            <path d="M340 565 V470"
                                  stroke="#36b8ed" stroke-width="12"/>
                            <path d="M205 470 H170 V385"
                                  stroke="#36b8ed" stroke-width="12"/>
                          </g>
                
                
                <!-- Electric boiler: supply and return -->
                <g id="electric-boiler-pipes"
                   fill="none"
                   stroke-linecap="round"
                   stroke-linejoin="round">
                
                    <!-- Подача електрокотла до опалювальних контурів -->
                    <path d="M210 525 H300 V150 H675"
                          stroke="#ff684f"
                          stroke-width="5"
                          stroke-dasharray="10 7"/>
                
                    <!-- Обратка від системи через зовнішній насос до електрокотла -->
                    <path d="M340 565 V610 H260"
                          stroke="#36b8ed"
                          stroke-width="5"
                          stroke-dasharray="10 7"/>
                
                    <path d="M210 585 H225 V610 H260"
                          stroke="#36b8ed"
                          stroke-width="5"
                          stroke-dasharray="10 7"/>
                </g>
                
                <!-- Зовнішній циркуляційний насос електрокотла -->
                <g id="electric-boiler-pump" transform="translate(265 610)">
                    <circle class="br-pump-ring pump-off"
                            r="19"
                            fill="#132237"
                            stroke="#22c55e"
                            stroke-width="3"/>
                
                    <circle r="12"
                            fill="#1d3047"
                            stroke="#71869d"
                            stroke-width="1.5"/>
                
                    <path d="M-5 -7 L7 0 L-5 7 Z"
                          fill="#ff866d"/>
                </g>
                
                <!-- Напрямок потоку -->
                <g id="electric-boiler-flow-arrows">
                    <!-- Подача -->
                    <path d="M290 350 L300 330 L310 350 Z"
                          fill="#ffb4a7"/>
                
                    <!-- Обратка до електрокотла -->
                    <path d="M275 600 L255 610 L275 620 Z"
                          fill="#8bdcff"/>
                </g>
                
                <!-- Напрямки руху теплоносія електрокотла -->
                
                </g>
                
                
                          <!-- Flow direction arrows -->
                          <g fill="#ffb4a7">
                            <path d="M285 210 l18 10 -18 10z"/>
                            <path d="M650 140 l18 10 -18 10z"/>
                            <path d="M745 290 l18 10 -18 10z"/>
                            <path d="M745 475 l18 10 -18 10z"/>
                          </g>
                
                          <g fill="#8bdcff">
                            <path d="M515 555 l-18 10 18 10z"/>
                            <path d="M255 460 l-18 10 18 10z"/>
                          </g>
                
                          <!-- Boiler -->
                          <g filter="url(#br-shadow)">
                            <rect x="55" y="180" width="155" height="220"
                                  rx="18" fill="url(#br-boiler)"
                                  stroke="#526a83" stroke-width="2"/>
                
                            <rect x="72" y="198" width="121" height="144"
                                  rx="10" fill="#111e2e" stroke="#3b526b"/>
                
                            <path d="M133 220 C126 244 104 250 113 275
                                     C120 295 149 296 159 276
                                     C169 256 149 244 145 229
                                     C141 244 135 247 133 220Z"
                                  fill="#ff7649"/>
                
                            <path d="M137 253 C131 266 127 272 134 282
                                     C141 291 153 283 151 273
                                     C149 266 142 262 137 253Z"
                                  fill="#ffd18a"/>
                
                            <text x="132" y="366" text-anchor="middle"
                                  fill="#e8f0fa" font-size="17" font-weight="700">
                              ТВЕРДОПАЛИВНИЙ
                            </text>
                
                            <text x="132" y="388" text-anchor="middle"
                                  fill="#e8f0fa" font-size="17" font-weight="700">
                              КОТЕЛ
                            </text>
                
                            <rect x="84" y="414" width="96" height="45"
                                  rx="10" fill="#17283b" stroke="#354d65"/>
                
                            <rect x="84" y="75" width="96" height="100"
                                  rx="10" fill="#47283b" stroke="#254d65"/>
                
                            <rect x="190" y="160" width="96" height="45"
                                  rx="10" fill="#17283b" stroke="#354d65"/>
                
                            <text x="238" y="178" text-anchor="middle"
                                  fill="#9db1c8" font-size="12">ПОДАЧА</text>
                
                            <text id="boiler-supply-value"
                                  x="238" y="198" text-anchor="middle"
                                  fill="#ffffff" font-size="16" font-weight="700">— °C</text>
                
                            <text x="132" y="433" text-anchor="middle"
                                  fill="#9db1c8" font-size="12">ЗВОРОТКА</text>
                
                            <text id="boiler-return-value"
                                  x="132" y="451" text-anchor="middle"
                                  fill="#ffffff" font-size="16" font-weight="700">— °C</text>
                
                            <text x="132" y="90" text-anchor="middle"
                                  fill="#9db1c8" font-size="12">ДИМОХІД</text>
                
                            <text id="flue-value"
                                  x="132" y="110" text-anchor="middle"
                                  fill="#ffffff" font-size="16" font-weight="700">— °C</text>
                          </g>
                
                          <!-- Boiler circulation pump -->
                          <g id="boiler-pump" transform="translate(250 470)">
                            <circle class="br-pump-ring pump-off"
                                    r="27" fill="#132237"
                                    stroke="#22c55e" stroke-width="3"/>
                
                            <circle r="18" fill="#1d3047"
                                    stroke="#71869d" stroke-width="1.5"/>
                
                            <path d="M-7 -11 L13 0 L-7 11Z"
                                  fill="#ff866d"/>
                
                          </g>
                
                
                <!-- Electric boiler -->
                <g id="electric-boiler">
                    <rect x="55" y="500" width="155" height="125"
                          rx="16"
                          fill="#142338"
                          stroke="#526a83"
                          stroke-width="2"/>
                
                    <rect x="72" y="515" width="121" height="48"
                          rx="8"
                          fill="#1d3047"
                          stroke="#647d96"
                          stroke-width="1.5"/>
                
                    <!-- Символ електронагріву -->
                    <path d="M137 518 L119 540 H132 L125 558 L151 532 H137 Z"
                          fill="#fbbf24"/>
                
                    <text x="132" y="584"
                          text-anchor="middle"
                          fill="#e6eef8"
                          font-size="14"
                          font-weight="700">
                        ЕЛЕКТРОКОТЕЛ
                    </text>
                
                    <text x="132" y="605"
                          text-anchor="middle"
                          fill="#aebed0"
                          font-size="13">
                        7,5 кВт
                    </text>
                
                    <!-- Температура подачі -->
                    <text x="245" y="540"
                          text-anchor="middle"
                          fill="#9db1c8"
                          font-size="11">ПОДАЧА</text>
                
                    <text id="electric-boiler-supply-value"
                          x="245" y="560"
                          text-anchor="middle"
                          fill="#ffffff"
                          font-size="15"
                          font-weight="700">— °C</text>
                
                       <!-- Температура зворотки -->
                    <text id="electric-boiler-return-value"
                          x="230" y="640"
                          text-anchor="middle"
                          fill="#ffffff"
                          font-size="15"
                          font-weight="700">— °C</text>
                
                    <!-- Індикатор запиту на нагрівання -->
                    <circle id="electric-boiler-heat-led"
                            cx="190" cy="510" r="7"
                            fill="#22c55e"
                            stroke="#ffffff"
                            stroke-width="1.5"/>
                </g>
                
                
                          <!-- Accumulator -->
                          <g filter="url(#br-shadow)">
                            <rect x="430" y="92" width="160" height="450"
                                  rx="34" fill="url(#br-tank)"
                                  stroke="#7890a8" stroke-width="3"/>
                
                            <ellipse cx="510" cy="100" rx="77" ry="15"
                                     fill="#31475d" stroke="#7890a8" stroke-width="2"/>
                
                            <rect x="444" y="115" width="132" height="78"
                                  rx="12" fill="#ef6250"/>
                
                            <rect x="444" y="198" width="132" height="78"
                                  rx="12" fill="#f28c48"/>
                
                            <rect x="444" y="281" width="132" height="78"
                                  rx="12" fill="#e8bd56"/>
                
                            <rect x="444" y="364" width="132" height="78"
                                  rx="12" fill="#4e9ed2"/>
                
                            <rect x="444" y="447" width="132" height="78"
                                  rx="12" fill="#2879b6"/>
                
                            <g fill="#101b2a" font-size="15" font-weight="700">
                              <text x="510" y="143" text-anchor="middle">ВЕРХНЯ ЗОНА</text>
                
                              <text id="accumulator-top-value"
                                    x="510" y="166" text-anchor="middle"
                                    font-size="23">— °C</text>
                
                              <text x="510" y="226" text-anchor="middle">2 ЗОНА</text>
                
                              <text id="accumulator-top-zone-value"
                                    x="510" y="249" text-anchor="middle"
                                    font-size="23">— °C</text>
                
                              <text x="510" y="309" text-anchor="middle">СЕРЕДИНА</text>
                
                              <text id="accumulator-middle-value"
                                    x="510" y="332" text-anchor="middle"
                                    font-size="23">— °C</text>
                
                              <text x="510" y="392" text-anchor="middle">4 ЗОНА</text>
                
                              <text id="accumulator-bottom-zone-value"
                                    x="510" y="415" text-anchor="middle"
                                    font-size="23">— °C</text>
                
                              <text x="510" y="475" text-anchor="middle">НИЖНЯ ЗОНА</text>
                
                              <text id="accumulator-bottom-value"
                                    x="510" y="498" text-anchor="middle"
                                    font-size="23">— °C</text>
                            </g>
                
                            <text x="510" y="65" text-anchor="middle"
                                                         fill="#aebed0" font-size="14">
                                                     РІВЕНЬ ЗАРЯДУ
                                                   </text>
                
                                               <text id="accumulator-charge-value"
                                                                     x="510" y="105"
                                                                     text-anchor="middle"
                                                                     fill="#06b6d4"
                                                                     font-size="22"
                                                                     font-weight="700">
                                                                   — %
                                                               </text>
                
                                                   <text x="510" y="615" text-anchor="middle"
                                                         fill="#f0f5fb" font-size="19" font-weight="700">
                                                     ТЕПЛОАКУМУЛЯТОР
                                                   </text>
                          </g>
                
                          <!-- Radiators -->
                          <g filter="url(#br-shadow)">
                            <rect x="760" y="72" width="275" height="128"
                                  rx="16" fill="#142338"
                                  stroke="#526a83" stroke-width="2"/>
                
                            <text x="897" y="98" text-anchor="middle"
                                  fill="#e6eef8" font-size="20" font-weight="700">
                              РАДІАТОРИ
                            </text>
                            <circle id="radiator-heat-led"
                                                      cx="1000" cy="98" r="7"
                                                      fill="#22c55e"
                                                      stroke="#ffffff" stroke-width="1.5"/>
                
                
                            <g fill="#71869b" stroke="#9aafc2" stroke-width="1.5">
                              <rect x="785" y="116" width="25" height="57" rx="5"/>
                              <rect x="814" y="116" width="25" height="57" rx="5"/>
                              <rect x="843" y="116" width="25" height="57" rx="5"/>
                              <rect x="872" y="116" width="25" height="57" rx="5"/>
                              <rect x="901" y="116" width="25" height="57" rx="5"/>
                              <rect x="930" y="116" width="25" height="57" rx="5"/>
                              <rect x="959" y="116" width="25" height="57" rx="5"/>
                              <rect x="988" y="116" width="25" height="57" rx="5"/>
                            </g>
                
                            <text x="785" y="190"
                                  fill="#aebed0" font-size="12">Подача:</text>
                
                            <text id="radiator-input-value"
                                  x="850" y="190"
                                  fill="#ffffff" font-size="13" font-weight="700">— °C</text>
                
                            <text x="910" y="190"
                                  fill="#aebed0" font-size="12">Обратка:</text>
                
                            <text id="radiator-output-value"
                                  x="1025" y="190" text-anchor="end"
                                  fill="#ffffff" font-size="13" font-weight="700">— °C</text>
                          </g>
                
                <!-- Underfloor heating -->
                <g filter="url(#br-shadow)">
                    <rect x="760" y="252" width="275" height="145"
                          rx="16" fill="#142338"
                          stroke="#526a83" stroke-width="2"/>
                
                    <text x="897" y="280" text-anchor="middle"
                          fill="#e6eef8" font-size="20" font-weight="700">
                        ТЕПЛА ПІДЛОГА
                    </text>
                    <circle id="floor-heat-led"
                                            cx="1000" cy="280" r="7"
                                            fill="#22c55e"
                                            stroke="#ffffff" stroke-width="1.5"/>
                
                    <g fill="none" stroke="#7ec9e8"
                       stroke-width="5" stroke-linecap="round">
                        <path d="M790 307 H1005 V324 H790
                                 V341 H1005 V358 H790 V375 H1005"/>
                    </g>
                
                    <text x="785" y="389"
                          fill="#aebed0" font-size="12">
                        Подача:
                    </text>
                
                    <text id="floor-input-value"
                          x="850" y="389"
                          fill="#ffffff" font-size="13" font-weight="700">
                        — °C
                    </text>
                
                    <text x="915" y="389"
                          fill="#aebed0" font-size="12">
                        Обратка:
                    </text>
                
                    <text id="floor-output-value"
                          x="1025" y="389" text-anchor="end"
                          fill="#ffffff" font-size="13" font-weight="700">
                        — °C
                    </text>
                </g>
                
                          <!-- DHW cylinder -->
                                              <g filter="url(#br-shadow)">
                                                  <rect x="760" y="444" width="275" height="150"
                                                        rx="16" fill="#142338"
                                                        stroke="#526a83" stroke-width="2"/>
                
                                                  <text x="897" y="471" text-anchor="middle"
                                                        fill="#e6eef8" font-size="20" font-weight="700">
                                                      БОЙЛЕР ГВП
                                                  </text>
                                                  <circle id="dhw-heat-led"
                                                                          cx="1000" cy="471" r="7"
                                                                          fill="#22c55e"
                                                                          stroke="#ffffff" stroke-width="1.5"/>
                
                
                
                                                  <rect x="850" y="486" width="94" height="83"
                                                        rx="20" fill="#253c53"
                                                        stroke="#829bb2" stroke-width="2"/>
                
                                                  <ellipse cx="897" cy="494" rx="43" ry="9"
                                                           fill="#344e67"
                                                           stroke="#829bb2" stroke-width="1.5"/>
                
                                                  <path d="M866 515 H928 M866 530 H928 M866 545 H928"
                                                        stroke="#ef785d" stroke-width="5"
                                                        stroke-linecap="round"/>
                
                                                  <text x="775" y="585"
                                                        fill="#aebed0" font-size="12">
                                                      Подача:
                                                  </text>
                
                                                  <text id="dhw-supply-value"
                                                        x="835" y="585"
                                                        fill="#ffffff" font-size="13" font-weight="700">
                                                      — °C
                                                  </text>
                
                                                  <text x="905" y="585"
                                                        fill="#aebed0" font-size="12">
                                                      Обратка:
                                                  </text>
                
                                                  <text id="dhw-temperature-value"
                                                        x="1025" y="585" text-anchor="end"
                                                        fill="#ffffff" font-size="13" font-weight="700">
                                                      — °C
                                                  </text>
                                              </g>
                
                          <!-- Radiator pump -->
                          <g id="radiator-pump" transform="translate(700 120)">
                            <circle class="br-pump-ring pump-off"
                                    r="23" fill="#132237"
                                    stroke="#22c55e" stroke-width="4"/>
                
                            <circle r="16" fill="#1d3047"
                                    stroke="#71869d" stroke-width="1.5"/>
                
                            <path d="M-6 -10 L12 0 L-6 10Z"
                                  fill="#ff866d"/>
                          </g>
                
                          <!-- Underfloor pump -->
                          <g id="floor-pump" transform="translate(700 300)">
                            <circle class="br-pump-ring pump-off"
                                    r="23" fill="#132237"
                                    stroke="#22c55e" stroke-width="4"/>
                
                            <circle r="16" fill="#1d3047"
                                    stroke="#71869d" stroke-width="1.5"/>
                
                            <path d="M-6 -10 L12 0 L-6 10Z"
                                  fill="#ff866d"/>
                          </g>
                
                          <!-- DHW pump -->
                          <g id="dhw-pump" transform="translate(700 485)">
                            <circle class="br-pump-ring pump-off"
                                    r="23" fill="#132237"
                                    stroke="#22c55e" stroke-width="4"/>
                
                            <circle r="16" fill="#1d3047"
                                    stroke="#71869d" stroke-width="1.5"/>
                
                            <path d="M-6 -10 L12 0 L-6 10Z"
                                  fill="#ff866d"/>
                          </g>
                
                          <text x="52" y="54" fill="#9fb2c7"
                                font-size="15" letter-spacing="2">
                            HOME HEATING / BOILER ROOM
                          </text>
                
                          <text x="1045" y="625" text-anchor="end"
                                fill="#667d95" font-size="13">
                            СХЕМА ДЛЯ ВІЗУАЛІЗАЦІЇ · НЕ ДЛЯ КЕРУВАННЯ
                          </text>
                        </svg>
                """;

        schematic.getElement().executeJs(
                "this.innerHTML = $0",
                svg
        );
    }

    // =========================================================
    // ATTACH / DETACH
    // =========================================================

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);

        UI ui = event.getUI();

        renderSchematic();

        refreshTelemetryData();

        ui.setPollInterval(10000);

        if (pollRegistration != null) {
            pollRegistration.remove();
        }

        pollRegistration = ui.addPollListener(
                pollEvent -> refreshTelemetryData()
        );
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        super.onDetach(event);
    }

    // =========================================================
    // LIVE TELEMETRY
    // =========================================================

    /**
     * Отримує останні значення через TelemetryService.
     * MQTT обробляє Spring Boot; Vaadin не підключається до брокера напряму.
     */
    private void refreshTelemetryData() {
        try {
            var readings = telemetryService.getTelemetryReadings(DEVICE_ID);

            if (readings.isEmpty()) {
                clearTelemetryData();

                statusTitle.setText("НЕМАЄ АКТУАЛЬНИХ ДАНИХ");
                statusDescription.setText(
                        "Очікування телеметрії від " + DEVICE_ID
                );

                return;
            }

            Map<String, TelemetryReadingResponse> bySensorId =
                    new LinkedHashMap<>();

            for (TelemetryReadingResponse reading : readings) {
                if (reading.sensorId() != null) {
                    bySensorId.put(reading.sensorId(), reading);
                }
            }

            setNumber(bySensorId, "boiler_supply", value -> boilerSupply = value);
            setNumber(bySensorId, "boiler_return", value -> boilerReturn = value);
            setNumber(bySensorId, "chimney_surface_temperature", value -> flueTemperature = value);

            setNumber(bySensorId, "accumulator_01", value -> accumulatorTop = value);
            setNumber(bySensorId, "accumulator_02", value -> accumulatorZoneTop = value);
            setNumber(bySensorId, "accumulator_03", value -> accumulatorMiddle = value);
            setNumber(bySensorId, "accumulator_04", value -> accumulatorZoneBottom = value);
            setNumber(bySensorId, "accumulator_05", value -> accumulatorBottom = value);

            setNumber(bySensorId, "radiator_supply", value -> radiatorIn = value);
            setNumber(bySensorId, "radiator_return", value -> radiatorOut = value);
            setNumber(bySensorId, "underfloor_supply", value -> floorIn = value);
            setNumber(bySensorId, "underfloor_return", value -> floorOut = value);
            setNumber(bySensorId, "dhw_supply", value -> dhwSupply = value);
            setNumber(bySensorId, "dhw_temperature", value -> dhwTemperature = value);
            setNumber(bySensorId, "system_pressure", value -> systemPressure = value);
            setNumber(bySensorId, "electric_boiler_supply",
                    value -> electricBoilerSupply = value);
            setNumber(bySensorId, "electric_boiler_return",
                    value -> electricBoilerReturn = value);

            boilerPumpOn = getBoolean(bySensorId, "boiler_pump", false);
            radiatorPumpOn = getBoolean(bySensorId, "radiator_pump", false);
            floorPumpOn = getBoolean(bySensorId, "underfloor_pump", false);
            dhwPumpOn = getBoolean(bySensorId, "dhw_pump", false);
            electricBoilerPumpOn = getBoolean(bySensorId, "electric_boiler_pump", false);

            lastTelemetryAt = readings.stream()
                    .map(reading -> reading.receivedAt() != null
                            ? reading.receivedAt()
                            : reading.measuredAt())
                    .filter(java.util.Objects::nonNull)
                    .max(Instant::compareTo)
                    .orElse(lastTelemetryAt);

            boolean hasTelemetry = !bySensorId.isEmpty();
            statusTitle.setText(hasTelemetry ? "КОНТРОЛЕР НАДІСЛАВ ДАНІ"
                    : "ОЧІКУВАННЯ ДАНИХ");
            statusDescription.setText(hasTelemetry
                    ? "Пристрій " + DEVICE_ID + " · останнє оновлення: "
                    + formatLastUpdate(lastTelemetryAt)
                    : "Немає телеметрії від " + DEVICE_ID);

            // Запити на тепло та стан електрокотла контролер наразі не передає.
            updateTemperatureValues();
            updatePumpStates();
            updateSystemValues();
            updateSchematicTemperatures();
            updatePumpIndicators();
            updateHeatRequestIndicators();
            updateSchematicTemperatureColors();

        } catch (Exception ex) {
            statusTitle.setText("ПОМИЛКА ОТРИМАННЯ ДАНИХ");
            statusDescription.setText(
                    "Не вдалося прочитати телеметрію " + DEVICE_ID
            );
            updateTemperatureValues();
            updatePumpStates();
            updateSystemValues();
            updateSchematicTemperatures();
            updatePumpIndicators();
            updateHeatRequestIndicators();
            updateSchematicTemperatureColors();
        }
    }

    private void clearTelemetryData() {
        // Температури
        boilerSupply = Double.NaN;
        boilerReturn = Double.NaN;
        flueTemperature = Double.NaN;

        accumulatorTop = Double.NaN;
        accumulatorZoneTop = Double.NaN;
        accumulatorMiddle = Double.NaN;
        accumulatorZoneBottom = Double.NaN;
        accumulatorBottom = Double.NaN;

        radiatorIn = Double.NaN;
        radiatorOut = Double.NaN;
        floorIn = Double.NaN;
        floorOut = Double.NaN;
        dhwSupply = Double.NaN;
        dhwTemperature = Double.NaN;

        systemPressure = Double.NaN;
        electricBoilerSupply = Double.NaN;
        electricBoilerReturn = Double.NaN;

        accumulatorChargePercent = Double.NaN;
        lastTelemetryAt = null;

        // Стани насосів більше невідомі
        boilerPumpOn = false;
        radiatorPumpOn = false;
        floorPumpOn = false;
        dhwPumpOn = false;
        electricBoilerPumpOn = false;

        // Оновлюємо температури та системні значення
        updateTemperatureValues();
        updateSystemValues();
        updateSchematicTemperatures();
        updateSchematicTemperatureColors();

        // Для насосів показуємо невідомий стан, а не "Вимкнено"
        pumpStates.values().forEach(span -> {
            span.setText("—");
            span.removeClassName("br-pump-on");
            span.removeClassName("br-pump-off");
        });

        //updatePumpIndicators();
        schematic.getElement().executeJs("""
                    this.querySelectorAll('.br-pump-ring').forEach(ring => {
                        ring.setAttribute('stroke', '#64748b');
                        ring.classList.remove('pump-on', 'pump-off');
                    });
                """);

    }

    @FunctionalInterface
    private interface NumberConsumer {
        void accept(double value);
    }

    private void setNumber(
            Map<String, TelemetryReadingResponse> readings,
            String sensorId,
            NumberConsumer consumer
    ) {
        TelemetryReadingResponse reading = readings.get(sensorId);

        if (reading == null || reading.numericValue() == null) {
            consumer.accept(Double.NaN);
            return;
        }

        consumer.accept(reading.numericValue().doubleValue());
    }

    private boolean getBoolean(
            Map<String, TelemetryReadingResponse> readings,
            String sensorId,
            boolean defaultValue
    ) {
        TelemetryReadingResponse reading = readings.get(sensorId);
        return reading == null || reading.booleanValue() == null
                ? defaultValue
                : reading.booleanValue();
    }

    private String formatLastUpdate(Instant timestamp) {
        if (timestamp == null) {
            return "—";
        }
        return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(timestamp);
    }

    private void updateTemperatureValues() {
        setTemperature("boilerSupply", boilerSupply);
        setTemperature("boilerReturn", boilerReturn);
        setTemperature("flueTemperature", flueTemperature);
        setTemperature("accumulatorTop", accumulatorTop);
        setTemperature("accumulatorMiddle", accumulatorMiddle);
        setTemperature("accumulatorBottom", accumulatorBottom);
        setTemperature("radiatorIn", radiatorIn);
        setTemperature("radiatorOut", radiatorOut);
        setTemperature("floorIn", floorIn);
        setTemperature("floorOut", floorOut);
        setTemperature("dhwSupply", dhwSupply);
        setTemperature("dhwTemperature", dhwTemperature);
        setTemperature("electricBoilerSupply", electricBoilerSupply);
        setTemperature("electricBoilerReturn", electricBoilerReturn);
        setTemperature("accumulatorTopZone", accumulatorZoneTop);
        setTemperature("accumulatorBottomZone", accumulatorZoneBottom);
        calculateAccumulatorCharge();
    }

    private void updatePumpStates() {
        setPumpState("boilerPump", boilerPumpOn);
        setPumpState("radiatorPump", radiatorPumpOn);
        setPumpState("floorPump", floorPumpOn);
        setPumpState("dhwPump", dhwPumpOn);
        setPumpState("electricBoilerPump", electricBoilerPumpOn);
    }

    private void updateSystemValues() {
        Span pressure = systemValues.get("pressure");
        if (pressure != null) {
            pressure.setText(Double.isFinite(systemPressure)
                    ? String.format(Locale.US, "%.1f бар", systemPressure)
                    : "—");
        }

        Span lastUpdate = systemValues.get("lastUpdate");
        if (lastUpdate != null) {
            lastUpdate.setText(formatLastUpdate(lastTelemetryAt));
        }
    }

    // =========================================================
    // UPDATE TEMPERATURES
    // =========================================================

    private void setTemperature(String key, double value) {
        Span span = temperatureValues.get(key);

        if (span != null) {
            span.setText(Double.isFinite(value)
                    ? String.format(Locale.US, "%.1f", value)
                    : "—");
        }
    }

    private String formatTemperature(double value) {
        return Double.isFinite(value)
                ? String.format(Locale.US, "%.1f", value)
                : "—";
    }

    private void updateSchematicTemperatures() {
        schematic.getElement().executeJs("""
                        const values = {
                            'boiler-supply-value': $0,
                            'boiler-return-value': $1,
                            'flue-value': $2,
                        
                            'accumulator-top-value': $3,
                            'accumulator-middle-value': $4,
                            'accumulator-bottom-value': $5,
                        
                            'radiator-input-value': $6,
                            'radiator-output-value': $7,
                        
                            'floor-input-value': $8,
                            'floor-output-value': $9,
                        
                            'dhw-supply-value': $10,
                            'dhw-temperature-value': $11,
                            'electric-boiler-supply-value': $12,
                            'electric-boiler-return-value': $13,
                            'accumulator-top-zone-value': $14,
                            'accumulator-bottom-zone-value': $15
                        };
                        
                        for (const [id, value] of Object.entries(values)) {
                            const element = this.querySelector('#' + id);
                        
                            if (element) {
                                element.textContent = value + ' °C';
                            }
                        }
                        """,
                formatTemperature(boilerSupply),
                formatTemperature(boilerReturn),
                formatTemperature(flueTemperature),

                formatTemperature(accumulatorTop),
                formatTemperature(accumulatorMiddle),
                formatTemperature(accumulatorBottom),

                formatTemperature(radiatorIn),
                formatTemperature(radiatorOut),

                formatTemperature(floorIn),
                formatTemperature(floorOut),

                formatTemperature(dhwSupply),
                formatTemperature(dhwTemperature),
                formatTemperature(electricBoilerSupply),
                formatTemperature(electricBoilerReturn),
                formatTemperature(accumulatorZoneTop),
                formatTemperature(accumulatorZoneBottom)

        );
        schematic.getElement().executeJs("""
                        const charge = this.querySelector('#accumulator-charge-value');
                        
                        if (charge) {
                            const rawPercent = $0;
                            const percent = Number(rawPercent);

                            if (rawPercent === '—' || !Number.isFinite(percent)) {
                                charge.textContent = '— %';
                                charge.setAttribute('fill', '#9db1c8');
                            } else {
                                charge.textContent = percent + ' %';

                                let color;
                                if (percent < 20) {
                                    color = '#3b82f6'; // Синій — низький заряд
                                } else if (percent < 50) {
                                    color = '#06b6d4'; // Блакитний — заряд нижче половини
                                } else if (percent < 80) {
                                    color = '#f59e0b'; // Помаранчевий — середній заряд
                                } else {
                                    color = '#ef4444'; // Червоний — високий заряд
                                }
                                charge.setAttribute('fill', color);
                            }
                        }
                        """,
                Double.isFinite(accumulatorChargePercent)
                        ? String.format(Locale.US, "%.0f", accumulatorChargePercent)
                        : "—"
        );
    }

    // =========================================================
    // UPDATE PUMP INDICATORS
    // =========================================================

    private void updatePumpIndicators() {
        schematic.getElement().executeJs("""
                        const pumps = {
                            'boiler-pump': $0,
                            'radiator-pump': $1,
                            'floor-pump': $2,
                            'dhw-pump': $3,
                            'electric-boiler-pump': $4
                        };
                        
                        for (const [id, isOn] of Object.entries(pumps)) {
                            const pump = this.querySelector('#' + id);
                        
                            if (!pump) {
                                continue;
                            }
                        
                            const ring = pump.querySelector('.br-pump-ring');
                        
                            if (!ring) {
                                continue;
                            }
                        
                            // Червоний — увімкнений, зелений — вимкнений
                            ring.setAttribute(
                                'stroke',
                                isOn ? '#ef4444' : '#22c55e'
                            );
                        
                            ring.classList.toggle('pump-on', isOn);
                            ring.classList.toggle('pump-off', !isOn);
                        }
                        """,
                boilerPumpOn,
                radiatorPumpOn,
                floorPumpOn,
                dhwPumpOn,
                electricBoilerPumpOn
        );
    }

    // =========================================================
    // UPDATE PUMP STATUS TEXT
    // =========================================================

    private void setPumpState(String key, boolean isOn) {
        Span span = pumpStates.get(key);

        if (span != null) {
            span.setText(isOn ? "Увімкнено" : "Вимкнено");

            span.getElement().getClassList().remove("br-pump-on");
            span.getElement().getClassList().remove("br-pump-off");

            span.addClassName(
                    isOn ? "br-pump-on" : "br-pump-off"
            );
        }
    }

    // =========================================================
    // UPDATE HEAT REQUEST INDICATORS
    // =========================================================

    private void updateHeatRequestIndicators() {
        schematic.getElement().executeJs("""
                        const requests = {
                            'radiator-heat-led': $0,
                            'floor-heat-led': $1,
                            'dhw-heat-led': $2,
                            'electric-boiler-heat-led': $3
                        };
                        
                        for (const [id, requested] of Object.entries(requests)) {
                            const indicator = this.querySelector('#' + id);
                        
                            if (!indicator) {
                                continue;
                            }
                        
                            // Червоний — потрібне нагрівання.
                            // Зелений — нагрівання не потрібне.
                            indicator.setAttribute(
                                'fill',
                                requested ? '#ef4444' : '#22c55e'
                            );
                        }
                        """,
                radiatorHeatRequest,
                floorHeatRequest,
                dhwHeatRequest,
                electricBoilerHeatRequest
        );
    }

    // =========================================================
// CALCULATE ACCUMULATOR CHARGE
// =========================================================

    private void calculateAccumulatorCharge() {
        double sum = 0.0;
        int count = 0;

        double[] temperatures = {
                accumulatorTop,
                accumulatorZoneTop,
                accumulatorMiddle,
                accumulatorZoneBottom,
                accumulatorBottom
        };

        for (double temperature : temperatures) {
            if (Double.isFinite(temperature)) {
                sum += temperature;
                count++;
            }
        }

        if (count == 0) {
            accumulatorChargePercent = Double.NaN;
            return;
        }

        double averageTemperature = sum / count;
        accumulatorChargePercent = Math.max(
                0.0,
                Math.min(100.0, (averageTemperature - 20.0) / 60.0 * 100.0)
        );
    }

    private void updateSchematicTemperatureColors() {
        schematic.getElement().executeJs("""
                        const temperatures = [
                            {
                                id: 'boiler-supply-value',
                                value: $0,
                                warning: 85,
                                alarm: 90,
                                inclusive: true
                            },
                            {
                                id: 'flue-value',
                                value: $1,
                                warning: 180,
                                alarm: 200,
                                inclusive: false
                            },
                            {
                                id: 'floor-input-value',
                                value: $2,
                                warning: 35,
                                alarm: 40,
                                inclusive: false
                            },
                            {
                                id: 'boiler-return-value',
                                value: $3,
                                warningLow: 55,
                                warningHigh: 65,
                                alarmLow: 50,
                                alarmHigh: 70,
                                range: true
                            }
                        ];
                        
                        for (const item of temperatures) {
                            const element = this.querySelector('#' + item.id);
                        
                            if (!element) {
                                continue;
                            }
                        
                            let state = 'temperature-normal';
                        
                            if (item.range) {
                                // Зворотка твердопаливного котла:
                                // 55–65 °C — норма
                                // 50–55 °C або 65–70 °C — попередження
                                // Нижче 50 °C або вище 70 °C — аварія
                        
                                if (
                                    item.value < item.alarmLow ||
                                    item.value > item.alarmHigh
                                ) {
                                    state = 'temperature-alarm';
                                } else if (
                                    item.value < item.warningLow ||
                                    item.value > item.warningHigh
                                ) {
                                    state = 'temperature-warning';
                                }
                            } else {
                                if (
                                    item.inclusive
                                        ? item.value >= item.alarm
                                        : item.value > item.alarm
                                ) {
                                    state = 'temperature-alarm';
                                } else if (item.value >= item.warning) {
                                    state = 'temperature-warning';
                                }
                            }
                        
                            element.classList.remove(
                                'temperature-normal',
                                'temperature-warning',
                                'temperature-alarm'
                            );
                        
                            element.classList.add(state);
                        }
                        """,
                boilerSupply,
                flueTemperature,
                floorIn,
                boilerReturn
        );
    }
}
