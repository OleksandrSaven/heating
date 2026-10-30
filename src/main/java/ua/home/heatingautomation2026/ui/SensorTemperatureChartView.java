package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.repository.SensorRepository;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Route(value = "sensor/:sensorId", layout = MainLayout.class)
@PageTitle("Графік датчика | Home Heating")
public class SensorTemperatureChartView extends VerticalLayout
        implements BeforeEnterObserver {

    private static final int MAX_READINGS = 500;

    private static final int CHART_WIDTH = 1000;
    private static final int CHART_HEIGHT = 440;

    private static final int PADDING_LEFT = 75;
    private static final int PADDING_RIGHT = 30;
    private static final int PADDING_TOP = 35;
    private static final int PADDING_BOTTOM = 65;

    private static final String TEXT_COLOR = "#cbd5e1";
    private static final String GRID_COLOR = "#334155";
    private static final String LINE_COLOR = "#60a5fa";
    private static final String BACKGROUND_COLOR = "#172033";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withLocale(Locale.forLanguageTag("uk-UA"))
                    .withZone(ZoneId.systemDefault());

    private final SensorRepository sensorRepository;
    private final TelemetryService telemetryService;

    private String sensorId;

    private final H1 title = new H1("Графік датчика");
    private final Paragraph sensorInfo = new Paragraph();
    private final Paragraph readingsInfo = new Paragraph();

    private final Div chartContainer = new Div();

    private final Button backButton =
            new Button("← Назад", event -> navigateBack());

    private final Button refreshButton =
            new Button("Оновити", event -> refreshChart());

    public SensorTemperatureChartView(
            SensorRepository sensorRepository,
            TelemetryService telemetryService
    ) {
        this.sensorRepository = sensorRepository;
        this.telemetryService = telemetryService;

        addClassName("sensor-chart-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureComponents();
        buildLayout();
    }

    private void configureComponents() {
        title.addClassName("sensor-chart-title");

        sensorInfo.addClassName("sensor-chart-info");
        readingsInfo.addClassName("sensor-chart-readings-info");

        backButton.addClassName("sensor-chart-back-button");
        refreshButton.addClassName("sensor-chart-refresh-button");

        chartContainer.addClassName("sensor-chart-container");
        chartContainer.setWidthFull();

        backButton.getElement()
                .setAttribute("aria-label", "Повернутися до попередньої сторінки");

        refreshButton.getElement()
                .setAttribute("aria-label", "Оновити графік вимірювань");
    }

    private void buildLayout() {
        HorizontalLayout header =
                new HorizontalLayout(backButton, title, refreshButton);

        header.addClassName("sensor-chart-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setPadding(false);
        header.setSpacing(true);
        header.expand(title);

        Div infoCard = new Div(sensorInfo);
        infoCard.addClassName("sensor-chart-info-card");
        infoCard.setWidthFull();

        H2 chartTitle = new H2("Історія вимірювань");
        chartTitle.addClassName("sensor-chart-section-title");

        Div chartCard = new Div(chartTitle, readingsInfo, chartContainer);
        chartCard.addClassName("sensor-chart-card");
        chartCard.setWidthFull();

        add(header, infoCard, chartCard);

        setAlignItems(Alignment.STRETCH);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        sensorId = event.getRouteParameters()
                .get("sensorId")
                .orElse(null);

        if (sensorId == null || sensorId.isBlank()) {
            showError("Не вказано ідентифікатор датчика.");
            return;
        }

        refreshChart();
    }

    private void refreshChart() {
        if (sensorId == null || sensorId.isBlank()) {
            return;
        }

        try {
            Sensor sensor = sensorRepository.findBySensorIdWithParameterDefinition(sensorId)
                    .orElse(null);

            if (sensor == null) {
                showError("Датчик з ідентифікатором " + sensorId
                        + " не знайдено.");
                return;
            }

            updateSensorInfo(sensor);

            List<TelemetryReadingResponse> readings =
                    telemetryService.getSensorTelemetryReadings(sensorId);

            if (readings == null || readings.isEmpty()) {
                showEmptyChart(
                        "Для цього датчика поки немає вимірювань."
                );
                return;
            }

            List<TelemetryReadingResponse> sortedReadings =
                    readings.stream()
                            .sorted(Comparator.comparing(
                                    TelemetryReadingResponse::measuredAt
                            ))
                            .toList();

            if (sortedReadings.size() > MAX_READINGS) {
                sortedReadings = sortedReadings.subList(
                        sortedReadings.size() - MAX_READINGS,
                        sortedReadings.size()
                );
            }

            readingsInfo.setText(
                    "Кількість вимірювань: " + sortedReadings.size()
                            + ". Показано останні "
                            + MAX_READINGS
                            + " записів за наявності більшої історії."
            );

            String svg = buildSvgChart(sortedReadings, sensor);

            chartContainer.removeAll();
            chartContainer.getElement().setProperty("innerHTML", svg);

        } catch (Exception exception) {
            log.error(
                    "Не вдалося завантажити графік датчика {}",
                    sensorId,
                    exception
            );

            showError(
                    "Не вдалося завантажити дані. "
                            + "Перевірте підключення та спробуйте ще раз."
            );
        }
    }

    private void updateSensorInfo(Sensor sensor) {
        String name = sensor.getSensorName() != null
                ? sensor.getSensorName()
                : sensor.getSensorId();

        String parameterName = sensor.getParameterDefinition() != null
                && sensor.getParameterDefinition().getName() != null
                ? sensor.getParameterDefinition().getName()
                : "Не визначено";

        String unit = sensor.getParameterDefinition() != null
                && sensor.getParameterDefinition().getUnit() != null
                ? sensor.getParameterDefinition().getUnit()
                : "";

        sensorInfo.setText(
                "Датчик: " + name
                        + " | ID: " + sensor.getSensorId()
                        + " | Параметр: " + parameterName
                        + (unit.isBlank() ? "" : " (" + unit + ")")
        );
    }

    private String buildSvgChart(
            List<TelemetryReadingResponse> readings,
            Sensor sensor
    ) {
        List<TelemetryReadingResponse> numericReadings =
                readings.stream()
                        .filter(reading -> reading.numericValue() != null)
                        .toList();

        if (numericReadings.isEmpty()) {
            return "";
        }

        double minValue = numericReadings.stream()
                .mapToDouble(reading -> reading.numericValue().doubleValue())
                .min()
                .orElse(0.0);

        double maxValue = numericReadings.stream()
                .mapToDouble(reading -> reading.numericValue().doubleValue())
                .max()
                .orElse(1.0);

        if (Double.compare(minValue, maxValue) == 0) {
            minValue -= 1.0;
            maxValue += 1.0;
        } else {
            double padding = (maxValue - minValue) * 0.1;
            minValue -= padding;
            maxValue += padding;
        }

        int plotWidth = CHART_WIDTH - PADDING_LEFT - PADDING_RIGHT;
        int plotHeight = CHART_HEIGHT - PADDING_TOP - PADDING_BOTTOM;

        StringBuilder svg = new StringBuilder();

        svg.append("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 %d %d"
                     width="%d"
                     height="%d"
                     role="img"
                     aria-label="Графік історії вимірювань датчика"
                     style="font-family: Arial, sans-serif; max-width: none;">
                """.formatted(
                CHART_WIDTH,
                CHART_HEIGHT,
                CHART_WIDTH,
                CHART_HEIGHT
        ));

        svg.append("""
                <rect x="0" y="0" width="%d" height="%d"
                      rx="10" fill="%s"/>
                """.formatted(
                CHART_WIDTH,
                CHART_HEIGHT,
                BACKGROUND_COLOR
        ));

        // Горизонтальні лінії сітки та підписи значень.
        int horizontalLines = 5;

        for (int i = 0; i <= horizontalLines; i++) {
            double ratio = (double) i / horizontalLines;

            double y = PADDING_TOP + ratio * plotHeight;
            double value = maxValue - ratio * (maxValue - minValue);

            svg.append("""
                    <line x1="%d" y1="%.2f"
                          x2="%d" y2="%.2f"
                          stroke="%s" stroke-width="1"/>
                    """.formatted(
                    PADDING_LEFT,
                    y,
                    CHART_WIDTH - PADDING_RIGHT,
                    y,
                    GRID_COLOR
            ));

            svg.append("""
                    <text x="%d" y="%.2f"
                          fill="%s"
                          font-size="12"
                          text-anchor="end"
                          dominant-baseline="middle">%s</text>
                    """.formatted(
                    PADDING_LEFT - 10,
                    y,
                    TEXT_COLOR,
                    escapeXml(String.format(Locale.US, "%.2f", value))
            ));
        }

        // Вертикальні лінії сітки та часові позначки.
        int verticalLines = Math.min(5, numericReadings.size() - 1);

        if (verticalLines > 0) {
            for (int i = 0; i <= verticalLines; i++) {
                double ratio = (double) i / verticalLines;

                double x = PADDING_LEFT + ratio * plotWidth;

                svg.append("""
                        <line x1="%.2f" y1="%d"
                              x2="%.2f" y2="%d"
                              stroke="%s" stroke-width="1"/>
                        """.formatted(
                        x,
                        PADDING_TOP,
                        x,
                        CHART_HEIGHT - PADDING_BOTTOM,
                        GRID_COLOR
                ));

                int index = (int) Math.round(
                        ratio * (numericReadings.size() - 1)
                );

                TelemetryReadingResponse reading =
                        numericReadings.get(index);

                String timestamp = reading.numericValue() == null
                        ? ""
                        : DATE_FORMATTER.format(reading.measuredAt());

                svg.append("""
                        <text x="%.2f" y="%d"
                              fill="%s"
                              font-size="11"
                              text-anchor="middle"
                              transform="rotate(-25 %.2f %d)">%s</text>
                        """.formatted(
                        x,
                        CHART_HEIGHT - PADDING_BOTTOM + 22,
                        TEXT_COLOR,
                        x,
                        CHART_HEIGHT - PADDING_BOTTOM + 22,
                        escapeXml(timestamp)
                ));
            }
        }

        // Лінія графіка.
        StringBuilder points = new StringBuilder();

        for (int i = 0; i < numericReadings.size(); i++) {
            TelemetryReadingResponse reading = numericReadings.get(i);

            double x;

            if (numericReadings.size() == 1) {
                x = PADDING_LEFT + plotWidth / 2.0;
            } else {
                x = PADDING_LEFT
                        + (double) i / (numericReadings.size() - 1)
                        * plotWidth;
            }

            double value = reading.numericValue().doubleValue();

            double y = PADDING_TOP
                    + (maxValue - value) / (maxValue - minValue)
                    * plotHeight;

            points.append(String.format(
                    Locale.US,
                    "%.2f,%.2f ",
                    x,
                    y
            ));
        }

        svg.append("""
                <polyline points="%s"
                          fill="none"
                          stroke="%s"
                          stroke-width="3"
                          stroke-linejoin="round"
                          stroke-linecap="round"/>
                """.formatted(
                points,
                LINE_COLOR
        ));

        // Кружки для окремих вимірювань.
        for (int i = 0; i < numericReadings.size(); i++) {
            TelemetryReadingResponse reading = numericReadings.get(i);

            double x;

            if (numericReadings.size() == 1) {
                x = PADDING_LEFT + plotWidth / 2.0;
            } else {
                x = PADDING_LEFT
                        + (double) i / (numericReadings.size() - 1)
                        * plotWidth;
            }

            double value = reading.numericValue().doubleValue();

            double y = PADDING_TOP
                    + (maxValue - value) / (maxValue - minValue)
                    * plotHeight;

            String timestamp = reading.measuredAt() == null
                    ? ""
                    : DATE_FORMATTER.format(reading.measuredAt());

            String unit = sensor.getParameterDefinition() != null
                    && sensor.getParameterDefinition().getUnit() != null
                    ? sensor.getParameterDefinition().getUnit()
                    : "";

            String tooltip = timestamp
                    + " | "
                    + String.format(Locale.US, "%.2f", value)
                    + (unit.isBlank() ? "" : " " + unit);

            svg.append("""
                    <circle cx="%.2f" cy="%.2f" r="3.5"
                            fill="%s"
                            stroke="%s"
                            stroke-width="1.5">
                        <title>%s</title>
                    </circle>
                    """.formatted(
                    x,
                    y,
                    LINE_COLOR,
                    BACKGROUND_COLOR,
                    escapeXml(tooltip)
            ));
        }

        svg.append("</svg>");

        return svg.toString();
    }

    private void showEmptyChart(String message) {
        readingsInfo.setText("Немає даних для відображення.");

        chartContainer.removeAll();

        Paragraph paragraph = new Paragraph(message);
        paragraph.addClassName("sensor-chart-empty-message");

        chartContainer.add(paragraph);
    }

    private void showError(String message) {
        sensorInfo.setText(message);
        readingsInfo.setText("");
        chartContainer.removeAll();

        Paragraph paragraph = new Paragraph(message);
        paragraph.addClassName("sensor-chart-error-message");

        chartContainer.add(paragraph);
    }

    private void navigateBack() {
        UI ui = UI.getCurrent();

        if (ui == null) {
            return;
        }

        ui.getPage().getHistory().back();
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
}
