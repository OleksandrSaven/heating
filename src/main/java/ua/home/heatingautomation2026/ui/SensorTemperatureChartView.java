package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.repository.SensorRepository;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Route(value = "sensor/:sensorId", layout = MainLayout.class)
@PageTitle("Графік датчика | Home Heating")
public class SensorTemperatureChartView extends VerticalLayout
        implements BeforeEnterObserver {

    private static final int CHART_WIDTH = 1000;
    private static final int CHART_HEIGHT = 440;

    private static final int LEFT = 85;
    private static final int RIGHT = 30;
    private static final int TOP = 35;
    private static final int BOTTOM = 75;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private static final DateTimeFormatter AXIS_DATE_FORMAT =
            DateTimeFormatter
                    .ofPattern("dd.MM HH:mm")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private final SensorRepository sensorRepository;
    private final TelemetryService telemetryService;

    private final H1 title = new H1("Графік датчика");
    private final Paragraph sensorInfo = new Paragraph();
    private final Span readingsInfo = new Span();
    private final Div chartContainer = new Div();

    private String currentSensorId;
    private Sensor currentSensor;

    public SensorTemperatureChartView(
            SensorRepository sensorRepository,
            TelemetryService telemetryService
    ) {
        this.sensorRepository = sensorRepository;
        this.telemetryService = telemetryService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("page-view");

        Button backButton = new Button(
                "← До пристрою",
                event -> {
                    if (currentSensor != null
                            && currentSensor.getDevice() != null) {

                        getUI().ifPresent(ui ->
                                ui.navigate(
                                        "device/"
                                                + currentSensor.getDevice()
                                                .getDeviceId()
                                )
                        );
                    }
                }
        );

        Button refreshButton = new Button(
                "Оновити графік",
                event -> refreshChart()
        );

        HorizontalLayout header =
                new HorizontalLayout(backButton, title, refreshButton);

        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);

        chartContainer.setWidthFull();
        chartContainer.getStyle()
                .set("overflow-x", "auto");

        add(
                header,
                sensorInfo,
                new H2("Історія вимірювань"),
                readingsInfo,
                chartContainer
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentSensorId = event.getRouteParameters()
                .get("sensorId")
                .orElse(null);

        if (currentSensorId == null || currentSensorId.isBlank()) {
            showError("Не вказано ID датчика.");
            return;
        }

        currentSensor = sensorRepository
                .findWithDetailsBySensorId(currentSensorId)
                .orElse(null);

        if (currentSensor == null) {
            showError(
                    "Датчик з ID " + currentSensorId + " не знайдено."
            );
            return;
        }

        refreshChart();
    }

    private void refreshChart() {
        if (currentSensor == null) {
            return;
        }

        String sensorName = currentSensor.getSensorName();

        String parameterName =
                currentSensor.getParameterDefinition() == null
                        ? "Невідомий параметр"
                        : currentSensor.getParameterDefinition().getName();

        String unit =
                currentSensor.getParameterDefinition() == null
                        || currentSensor.getParameterDefinition().getUnit() == null
                        ? ""
                        : currentSensor.getParameterDefinition().getUnit();

        title.setText(sensorName);

        sensorInfo.setText(
                "Датчик: " + sensorName
                        + " | ID: " + currentSensor.getSensorId()
                        + " | Параметр: " + parameterName
                        + (unit.isBlank() ? "" : " (" + unit + ")")
        );

        List<TelemetryReadingResponse> readings =
                telemetryService.getSensorTelemetryReadings(
                        currentSensorId
                );

        /*
         * Графік призначений для числових показників.
         * Значення boolean та string не відображаємо.
         */
        List<TelemetryReadingResponse> numericReadings =
                new ArrayList<>(
                        readings.stream()
                                .filter(reading ->
                                        reading.numericValue() != null
                                )
                                .toList()
                );

        /*
         * Репозиторій повертає записи від найновішого до найстарішого.
         * Для графіка потрібен зворотний порядок.
         */
        numericReadings.sort(
                Comparator.comparing(
                        this::getTimestamp,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                )
        );

        readingsInfo.setText(
                "Кількість точок: "
                        + numericReadings.size()
                        + " із максимум 500"
        );

        if (numericReadings.isEmpty()) {
            chartContainer.removeAll();

            chartContainer.add(
                    new Paragraph(
                            "Для цього датчика ще немає числових вимірювань."
                    )
            );

            return;
        }

        chartContainer.getElement().setProperty(
                "innerHTML",
                buildSvgChart(numericReadings, unit)
        );
    }

    private String buildSvgChart(
            List<TelemetryReadingResponse> readings,
            String unit
    ) {
        int plotWidth = CHART_WIDTH - LEFT - RIGHT;
        int plotHeight = CHART_HEIGHT - TOP - BOTTOM;

        List<BigDecimal> values = readings.stream()
                .map(TelemetryReadingResponse::numericValue)
                .toList();

        double minValue = values.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .min()
                .orElse(0.0);

        double maxValue = values.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .max()
                .orElse(1.0);

        /*
         * Якщо всі значення однакові, додаємо запас,
         * щоб точки не накладалися на межі графіка.
         */
        if (Double.compare(minValue, maxValue) == 0) {
            double padding = Math.max(Math.abs(minValue) * 0.05, 1.0);
            minValue -= padding;
            maxValue += padding;
        } else {
            double padding = (maxValue - minValue) * 0.08;
            minValue -= padding;
            maxValue += padding;
        }

        StringBuilder svg = new StringBuilder();

        svg.append("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 %d %d"
                     width="100%%"
                     style="min-width:650px;max-width:1200px;
                            font-family:Arial,sans-serif">
                """.formatted(CHART_WIDTH, CHART_HEIGHT));

        svg.append("""
                <rect width="100%%" height="100%%" fill="white"/>
                """);

        /*
         * Горизонтальні лінії сітки та підписи осі Y.
         */
        for (int i = 0; i <= 5; i++) {
            double ratio = i / 5.0;

            double y = TOP + ratio * plotHeight;

            double value = maxValue
                    - ratio * (maxValue - minValue);

            svg.append("""
                    <line x1="%d" y1="%.2f"
                          x2="%d" y2="%.2f"
                          stroke="#dddddd" stroke-width="1"/>
                    """.formatted(
                    LEFT,
                    y,
                    CHART_WIDTH - RIGHT,
                    y
            ));

            svg.append("""
                    <text x="%d" y="%.2f"
                          text-anchor="end"
                          font-size="13" fill="#444444">%s</text>
                    """.formatted(
                    LEFT - 10,
                    y + 4,
                    escapeXml(formatNumber(value))
            ));
        }

        /*
         * Вертикальна та горизонтальна осі.
         */
        svg.append("""
                <line x1="%d" y1="%d"
                      x2="%d" y2="%d"
                      stroke="#444444" stroke-width="1.5"/>
                <line x1="%d" y1="%d"
                      x2="%d" y2="%d"
                      stroke="#444444" stroke-width="1.5"/>
                """.formatted(
                LEFT,
                TOP,
                LEFT,
                TOP + plotHeight,
                LEFT,
                TOP + plotHeight,
                CHART_WIDTH - RIGHT,
                TOP + plotHeight
        ));

        /*
         * Формуємо координати точок.
         */
        StringBuilder points = new StringBuilder();

        for (int i = 0; i < readings.size(); i++) {
            double x = readings.size() == 1
                    ? LEFT + plotWidth / 2.0
                    : LEFT + (double) i / (readings.size() - 1)
                    * plotWidth;

            double value = readings.get(i)
                    .numericValue()
                    .doubleValue();

            double y = TOP
                    + (maxValue - value)
                    / (maxValue - minValue)
                    * plotHeight;

            points.append(
                    String.format(
                            Locale.US,
                            "%.2f,%.2f ",
                            x,
                            y
                    )
            );
        }

        /*
         * Лінія графіка.
         */
        svg.append("""
                <polyline points="%s"
                          fill="none"
                          stroke="#1976d2"
                          stroke-width="2.5"
                          stroke-linejoin="round"
                          stroke-linecap="round"/>
                """.formatted(points));

        /*
         * Точки та підказки з датою і значенням.
         */
        for (int i = 0; i < readings.size(); i++) {
            TelemetryReadingResponse reading = readings.get(i);

            double x = readings.size() == 1
                    ? LEFT + plotWidth / 2.0
                    : LEFT + (double) i / (readings.size() - 1)
                    * plotWidth;

            double value = reading.numericValue().doubleValue();

            double y = TOP
                    + (maxValue - value)
                    / (maxValue - minValue)
                    * plotHeight;

            String timestamp = formatTimestamp(getTimestamp(reading));

            String tooltip = timestamp
                    + " — "
                    + formatNumber(value)
                    + (unit == null || unit.isBlank() ? "" : " " + unit);

            svg.append("""
                    <circle cx="%.2f" cy="%.2f" r="3"
                            fill="#1976d2">
                        <title>%s</title>
                    </circle>
                    """.formatted(
                    x,
                    y,
                    escapeXml(tooltip)
            ));
        }

        /*
         * Підписи часу на початку, посередині та в кінці осі X.
         */
        int[] indices = {
                0,
                readings.size() / 2,
                readings.size() - 1
        };

        for (int index : indices) {
            TelemetryReadingResponse reading = readings.get(index);

            double x = readings.size() == 1
                    ? LEFT + plotWidth / 2.0
                    : LEFT + (double) index / (readings.size() - 1)
                    * plotWidth;

            Instant timestamp = getTimestamp(reading);

            String label = timestamp == null
                    ? "—"
                    : AXIS_DATE_FORMAT.format(timestamp);

            svg.append("""
                    <text x="%.2f" y="%d"
                          text-anchor="middle"
                          font-size="12" fill="#444444">%s</text>
                    """.formatted(
                    x,
                    TOP + plotHeight + 25,
                    escapeXml(label)
            ));
        }

        /*
         * Підпис одиниці вимірювання.
         */
        if (unit != null && !unit.isBlank()) {
            svg.append("""
                    <text x="15" y="%d"
                          font-size="13" fill="#444444">%s</text>
                    """.formatted(
                    TOP,
                    escapeXml(unit)
            ));
        }

        svg.append("</svg>");

        return svg.toString();
    }

    private Instant getTimestamp(TelemetryReadingResponse reading) {
        return reading.measuredAt() != null
                ? reading.measuredAt()
                : reading.receivedAt();
    }

    private String formatTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return "Час невідомий";
        }

        return DATE_FORMAT.format(timestamp);
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void showError(String message) {
        title.setText("Датчик недоступний");
        sensorInfo.setText(message);
        readingsInfo.setText("");
        chartContainer.removeAll();
        chartContainer.add(new Paragraph(message));
    }
}