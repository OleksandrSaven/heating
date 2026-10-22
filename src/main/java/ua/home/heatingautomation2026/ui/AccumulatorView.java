package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import ua.home.heatingautomation2026.entity.TelemetryReading;
import ua.home.heatingautomation2026.repository.TelemetryReadingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Route(value = "accumulator", layout = MainLayout.class)
@PageTitle("Теплоакумулятор | Home Heating")
public class AccumulatorView extends VerticalLayout {

    private static final String SENSOR_TOP = "accumulator_00";
    private static final String TOP_SENSOR = "accumulator_01";
    private static final String MIDDLE_SENSOR = "accumulator_02";
    private static final String BOTTOM_SENSOR = "accumulator_03";
    private static final String SENSOR_BUTTOM = "accumulator_04";


    /*
     * Початкові температурні межі для приблизного індикатора заряду.
     * Їх можна буде змінити відповідно до налаштувань системи.
     */
    private static final double MIN_TEMPERATURE = 20.0;
    private static final double MAX_TEMPERATURE = 80.0;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private final TelemetryReadingRepository telemetryRepository;

    private final Div dashboard = new Div();

    private Registration pollRegistration;

    public AccumulatorView(
            TelemetryReadingRepository telemetryRepository
    ) {
        this.telemetryRepository = telemetryRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        addClassName("page-view");

        H1 title = new H1("Теплоакумулятор");
        Paragraph subtitle = new Paragraph(
                "Температура та орієнтовний рівень заряду бака"
        );

        Button backButton = new Button(
                "<- До пристроїв",
                event -> event.getUI().navigate("devices")
        );

        Button refreshButton = new Button(
                "Оновити",
                event -> refreshData()
        );

        HorizontalLayout header =
                new HorizontalLayout(backButton, title, refreshButton);

        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);

        dashboard.setWidthFull();

        add(header, subtitle, dashboard);

        refreshData();
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);

        UI ui = event.getUI();

        ui.setPollInterval(5000);

        ui.addPollListener(pollEvent -> refreshData());
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        super.onDetach(event);
    }

    private void refreshData() {
        Optional<TelemetryReading> heigh = telemetryRepository
                .findTopBySensor_SensorIdOrderByReceivedAtDesc(SENSOR_TOP);

        Optional<TelemetryReading> top =
                telemetryRepository
                        .findTopBySensor_SensorIdOrderByReceivedAtDesc(
                                TOP_SENSOR
                        );

        Optional<TelemetryReading> middle =
                telemetryRepository
                        .findTopBySensor_SensorIdOrderByReceivedAtDesc(
                                MIDDLE_SENSOR
                        );

        Optional<TelemetryReading> bottom =
                telemetryRepository
                        .findTopBySensor_SensorIdOrderByReceivedAtDesc(
                                BOTTOM_SENSOR
                        );
        Optional<TelemetryReading> down =
                telemetryRepository
                        .findTopBySensor_SensorIdOrderByReceivedAtDesc(
                                SENSOR_BUTTOM
                        );



        Double heighValue = getNumericValue(heigh);
        Double topValue = getNumericValue(top);
        Double middleValue = getNumericValue(middle);
        Double bottomValue = getNumericValue(bottom);
        Double downValue = getNumericValue(down);

        Double charge = calculateCharge(
                topValue,
                middleValue,
                bottomValue
        );

        dashboard.getElement().setProperty(
                "innerHTML",
                buildDashboard(
                        heighValue,
                        topValue,
                        middleValue,
                        bottomValue,
                        downValue,
                        charge,
                        top.map(TelemetryReading::getReceivedAt)
                                .map(TIME_FORMAT::format)
                                .orElse(null),
                        middle.map(TelemetryReading::getReceivedAt)
                                .map(TIME_FORMAT::format)
                                .orElse(null),
                        bottom.map(TelemetryReading::getReceivedAt)
                                .map(TIME_FORMAT::format)
                                .orElse(null)
                )
        );
    }

    private Double getNumericValue(
            Optional<TelemetryReading> reading
    ) {
        return reading
                .map(TelemetryReading::getNumericValue)
                .map(BigDecimal::doubleValue)
                .orElse(null);
    }

    /**
     * Приблизна оцінка заряду за середньою температурою трьох зон.
     *
     * Це не точне вимірювання запасу теплової енергії.
     */
    /**
     * Орієнтовний заряд теплоакумулятора
     * з урахуванням температури трьох зон.
     *
     * 20 °C — зона розряджена.
     * 80 °C — зона повністю заряджена.
     */
    private Double calculateCharge(
            Double top,
            Double middle,
            Double bottom
    ) {
        if (top == null || middle == null || bottom == null) {
            return null;
        }

        double topCharge = calculateZoneCharge(top);
        double middleCharge = calculateZoneCharge(middle);
        double bottomCharge = calculateZoneCharge(bottom);

        return (topCharge + middleCharge + bottomCharge) / 3.0;
    }

    /**
     * Розрахунок заряду однієї зони.
     */
    private double calculateZoneCharge(double temperature) {
        double charge = (temperature - MIN_TEMPERATURE)
                / (MAX_TEMPERATURE - MIN_TEMPERATURE)
                * 100.0;

        return Math.max(0.0, Math.min(100.0, charge));
    }

    private String buildDashboard(
            Double topValue,
            Double top,
            Double middle,
            Double bottom,
            Double down,
            Double charge,
            String topTime,
            String middleTime,
            String bottomTime
    ) {
        String topColor = temperatureColor(top);
        String middleColor = temperatureColor(middle);
        String bottomColor = temperatureColor(bottom);

        String chargeText = charge == null
                ? "—"
                : String.format(
                        Locale.US,
                        "%.0f%%",
                        charge
                );

        String chargeWidth = charge == null
                ? "0%"
                : chargeText;

        String updatedAt = latestTimestamp(
                topTime,
                middleTime,
                bottomTime
        );

        return """
                <style>
                  .acc-root {
                    color: #e8f0ff;
                    font-family: Arial, sans-serif;
                  }

                  .acc-grid {
                    display: grid;
                    grid-template-columns:
                      minmax(320px, 1.1fr)
                      minmax(300px, 0.9fr);
                    gap: 20px;
                    align-items: stretch;
                  }

                  .acc-card {
                    background: linear-gradient(
                      145deg, #10213a, #0a1627
                    );
                    border: 1px solid #1e3554;
                    border-radius: 16px;
                    padding: 22px;
                    box-sizing: border-box;
                  }

                  .acc-heading {
                    font-size: 20px;
                    font-weight: 700;
                    margin-bottom: 18px;
                  }

                  .acc-tank-area {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 450px;
                    gap: 24px;
                  }

                  .acc-tank {
                    position: relative;
                    width: 150px;
                    height: 390px;
                    flex-shrink: 0;
                    border: 7px solid #7086a5;
                    border-radius: 48px;
                    overflow: hidden;
                    background: #102642;
                    box-shadow:
                      inset 0 0 18px #0009,
                      0 12px 35px #0005;
                  }

                  .acc-zone {
                    height: 33.3333%%;
                    transition: background 0.5s;
                    border-bottom: 1px solid #ffffff38;
                  }

                  .acc-top {
                    background: linear-gradient(
                      180deg, #ff3029, #ff6b21
                    );
                  }

                  .acc-middle {
                    background: linear-gradient(
                      180deg, #ffb51b, #f0d62c
                    );
                  }

                  .acc-bottom {
                    background: linear-gradient(
                      180deg, #13a8ed, #0753d8
                    );
                  }

                  .acc-levels {
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    gap: 18px;
                    min-width: 145px;
                  }

                  .acc-level {
                    border-left: 4px solid;
                    padding-left: 12px;
                  }

                  .acc-level-name {
                    font-size: 14px;
                    color: #a9c2e8;
                    margin-bottom: 7px;
                  }

                  .acc-temp {
                    font-size: 25px;
                    font-weight: 700;
                  }

                  .acc-meta {
                    font-size: 12px;
                    color: #8fa9ce;
                    margin-top: 6px;
                  }

                  .acc-charge {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 20px;
                    min-height: 170px;
                  }

                  .acc-ring {
                    width: 145px;
                    height: 145px;
                    border-radius: 50%%;
                    display: grid;
                    place-items: center;
                    background:
                      conic-gradient(
                        #13c69a var(--charge),
                        #203b59 0
                      );
                    position: relative;
                    flex-shrink: 0;
                  }

                  .acc-ring::before {
                    content: "";
                    position: absolute;
                    inset: 12px;
                    border-radius: 50%%;
                    background: #0d1d32;
                  }

                  .acc-ring-value {
                    position: relative;
                    font-size: 30px;
                    font-weight: 700;
                  }

                  .acc-charge-description {
                    color: #b6c9e6;
                    line-height: 1.6;
                  }

                  .acc-reading {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    padding: 15px 0;
                    border-bottom: 1px solid #203550;
                  }

                  .acc-dot {
                    width: 13px;
                    height: 13px;
                    border-radius: 50%%;
                    flex-shrink: 0;
                  }

                  .acc-reading-name {
                    flex: 1;
                    color: #b6c9e6;
                  }

                  .acc-reading-value {
                    font-size: 17px;
                    font-weight: 700;
                    white-space: nowrap;
                  }

                  .acc-note {
                    margin-top: 20px;
                    color: #9bb5d9;
                    line-height: 1.6;
                    font-size: 13px;
                  }

                  .acc-footer {
                    margin-top: 20px;
                    color: #91acd1;
                    font-size: 13px;
                  }

                  @media (max-width: 800px) {
                    .acc-grid {
                      grid-template-columns: 1fr;
                    }

                    .acc-tank-area {
                      gap: 15px;
                      min-height: 360px;
                    }

                    .acc-tank {
                      width: 105px;
                      height: 310px;
                    }

                    .acc-temp {
                      font-size: 20px;
                    }

                    .acc-levels {
                      min-width: 120px;
                    }

                    .acc-charge {
                      flex-wrap: wrap;
                    }
                  }
                </style>

                <div class="acc-root">
                  <div class="acc-grid">

                    <section class="acc-card">
                      <div class="acc-heading">
                        Теплоакумулятор · 1000 л
                      </div>

                      <div class="acc-tank-area">
                        <div class="acc-tank">
                          <div class="acc-zone acc-top"></div>
                          <div class="acc-zone acc-middle"></div>
                          <div class="acc-zone acc-bottom"></div>
                        </div>

                        <div class="acc-levels">
                          <div class="acc-level"
                               style="border-color:%s">
                            <div class="acc-level-name">
                              Верх · accumulator_01
                            </div>
                            <div class="acc-temp">%s</div>
                            <div class="acc-meta">%s</div>
                          </div>

                          <div class="acc-level"
                               style="border-color:%s">
                            <div class="acc-level-name">
                              Середина · accumulator_02
                            </div>
                            <div class="acc-temp">%s</div>
                            <div class="acc-meta">%s</div>
                          </div>

                          <div class="acc-level"
                               style="border-color:%s">
                            <div class="acc-level-name">
                              Низ · accumulator_03
                            </div>
                            <div class="acc-temp">%s</div>
                            <div class="acc-meta">%s</div>
                          </div>
                        </div>
                      </div>
                    </section>

                    <div>
                      <section class="acc-card">
                        <div class="acc-heading">Рівень заряду</div>

                        <div class="acc-charge">
                          <div class="acc-ring"
                               style="--charge:%s">
                            <div class="acc-ring-value">%s</div>
                          </div>

                          <div class="acc-charge-description">
                            Орієнтовний заряд<br>
                            теплоакумулятора
                          </div>
                        </div>
                      </section>

                      <section class="acc-card" style="margin-top:20px">
                        <div class="acc-heading">
                          Температури датчиків
                        </div>
                        <div class="acc-reading">
                          <span class="acc-dot"
                                style="background:%s"></span>
                          <span class="acc-reading-name">
                            accumulator_00 (самий верх)
                          </span>
                          <span class="acc-reading-value">%s</span>
                        </div>

                        <div class="acc-reading">
                          <span class="acc-dot"
                                style="background:%s"></span>
                          <span class="acc-reading-name">
                            accumulator_01 (верхній)
                          </span>
                          <span class="acc-reading-value">%s</span>
                        </div>
                
                        <div class="acc-reading">
                          <span class="acc-dot"
                                style="background:%s"></span>
                          <span class="acc-reading-name">
                            accumulator_02 (середній)
                          </span>
                          <span class="acc-reading-value">%s</span>
                        </div>

                        <div class="acc-reading">
                          <span class="acc-dot"
                                style="background:%s"></span>
                          <span class="acc-reading-name">
                            accumulator_03 (нижній)
                          </span>
                          <span class="acc-reading-value">%s</span>
                        </div>
                
                        <div class="acc-reading">
                          <span class="acc-dot"
                                style="background:%s"></span>
                          <span class="acc-reading-name">
                            accumulator_04 (самий низ)
                          </span>
                          <span class="acc-reading-value">%s</span>
                        </div>

                        <div class="acc-note">
                          Заряд розраховується приблизно за середньою
                          температурою трьох зон. Температурні межі:
                          20–80 °C. Для точнішої оцінки їх потрібно
                          налаштувати під вашу систему.
                        </div>
                      </section>
                    </div>
                  </div>

                  <div class="acc-footer">
                    Останнє оновлення: %s
                  </div>
                </div>
                """.formatted(
                topColor,
                formatTemperature(top),
                formatTime(topTime),
                middleColor,
                formatTemperature(middle),
                formatTime(middleTime),
                bottomColor,
                formatTemperature(bottom),
                formatTime(bottomTime),
                chargeWidth,
                chargeText,
                topColor,
                formatTemperature(topValue),
                middleColor,
                formatTemperature(top),
                middleColor,
                formatTemperature(middle),
                bottomColor,
                formatTemperature(bottom),
                bottomColor,
                formatTemperature(down),
                updatedAt
        );
    }

    private String temperatureColor(Double temperature) {
        if (temperature == null) {
            return "#64748b";
        }

        if (temperature >= 65) {
            return "#ff453a";
        }

        if (temperature >= 45) {
            return "#ffb51b";
        }

        return "#159bea";
    }

    private String formatTemperature(Double value) {
        if (value == null) {
            return "—";
        }

        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString() + " °C";
    }

    private String formatTime(String value) {
        return value == null ? "Немає даних" : value;
    }

    private String latestTimestamp(
            String top,
            String middle,
            String bottom
    ) {
        if (top == null && middle == null && bottom == null) {
            return "дані ще не отримані";
        }

        return "автоматичне оновлення кожні 5 секунд";
    }
}