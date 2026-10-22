package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import ua.home.heatingautomation2026.entity.TelemetryReading;
import ua.home.heatingautomation2026.repository.TelemetryReadingRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "telemetry-chart/:sensorId", layout = MainLayout.class)
@PageTitle("Графік температур | Home Heating")
public class TelemetryChartView extends VerticalLayout
        implements BeforeEnterObserver {

    private static final ZoneId ZONE =
            ZoneId.of("Europe/Kyiv");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM HH:mm")
                    .withZone(ZONE);

    private static final String[] COLORS = {
            "#e53935",
            "#fb8c00",
            "#1e88e5",
            "#43a047",
            "#8e24aa",
            "#00897b",
            "#6d4c41",
            "#3949ab",
            "#d81b60",
            "#546e7a",
            "#7cb342",
            "#f4511e"
    };

    private final TelemetryReadingRepository repository;

    private final H1 title =
            new H1("Температури всіх датчиків");

    private final Paragraph info =
            new Paragraph();

    private final Div chartContainer = new Div();

    private final ComboBox<String> periodSelector =
            new ComboBox<>("Період");

    private String selectedSensorId;

    private Duration selectedPeriod = Duration.ofHours(24);

    private Registration pollRegistration;

    public TelemetryChartView(
            TelemetryReadingRepository repository
    ) {
        this.repository = repository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("page-view");

        periodSelector.setItems(
                "30 хвилин",
                "1 година",
                "6 годин",
                "24 години"
        );

        periodSelector.setValue("24 години");
        periodSelector.setWidth("180px");

        periodSelector.addValueChangeListener(event -> {
            String value = event.getValue();

            if (value == null) {
                return;
            }

            selectedPeriod = switch (value) {
                case "30 хвилин" -> Duration.ofMinutes(30);
                case "1 година" -> Duration.ofHours(1);
                case "6 годин" -> Duration.ofHours(6);
                default -> Duration.ofHours(24);
            };

            refreshChart();
        });

        Button backButton = new Button(
                "← До пристрою",
                event -> getUI().ifPresent(ui -> ui.navigate("devices"))
        );

        Button refreshButton = new Button(
                "Оновити",
                event -> refreshChart()
        );

        HorizontalLayout toolbar =
                new HorizontalLayout(
                        backButton,
                        periodSelector,
                        refreshButton
                );

        toolbar.setAlignItems(Alignment.END);
        toolbar.setWidthFull();

        chartContainer.setWidthFull();

        chartContainer.getStyle()
                .set("overflow-x", "auto")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("padding", "12px")
                .set("box-sizing", "border-box");

        add(title, toolbar, info, chartContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        selectedSensorId = event.getRouteParameters()
                .get("sensorId")
                .orElse(null);

        refreshChart();
    }

    private void refreshChart() {
        Instant now = Instant.now();
        Instant from = now.minus(selectedPeriod);

        List<TelemetryReading> readings =
                repository.findTemperatureReadingsSince(from);

        Map<String, List<TelemetryReading>> series =
                new LinkedHashMap<>();

        for (TelemetryReading reading : readings) {
            String sensorId =
                    reading.getSensor().getSensorId();

            series.computeIfAbsent(
                    sensorId,
                    ignored -> new ArrayList<>()
            ).add(reading);
        }

        info.setText(
                "Датчик, з якого відкрито графік: "
                        + (selectedSensorId == null
                        ? "не вказано"
                        : selectedSensorId)
                        + ". Температурних датчиків із даними: "
                        + series.size()
                        + ". Період: "
                        + TIME_FORMAT.format(from)
                        + " — "
                        + TIME_FORMAT.format(now)
        );

        if (readings.isEmpty()) {
            chartContainer.getElement().setProperty(
                    "innerHTML",
                    "<p>За вибраний період немає вимірювань температури.</p>"
            );
            return;
        }

        chartContainer.getElement().setProperty(
                "innerHTML",
                buildSvg(series, from, now)
        );
    }

    private String buildSvg(
            Map<String, List<TelemetryReading>> series,
            Instant from,
            Instant to
    ) {
        final int width = 1100;
        final int height = 520;

        final int left = 75;
        final int right = 30;
        final int top = 30;
        final int bottom = 75;

        final int plotWidth = width - left - right;
        final int plotHeight = height - top - bottom;

        double minTemperature = Double.POSITIVE_INFINITY;
        double maxTemperature = Double.NEGATIVE_INFINITY;

        for (List<TelemetryReading> values : series.values()) {
            for (TelemetryReading reading : values) {
                double temperature =
                        reading.getNumericValue().doubleValue();

                minTemperature =
                        Math.min(minTemperature, temperature);

                maxTemperature =
                        Math.max(maxTemperature, temperature);
            }
        }

        if (minTemperature == maxTemperature) {
            minTemperature -= 5;
            maxTemperature += 5;
        } else {
            double padding =
                    (maxTemperature - minTemperature) * 0.1;

            minTemperature -= padding;
            maxTemperature += padding;
        }

        double temperatureRange =
                maxTemperature - minTemperature;

        long timeRangeMillis =
                Math.max(1, Duration.between(from, to).toMillis());

        StringBuilder svg = new StringBuilder();

        svg.append("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 %d %d"
                     width="100%%"
                     style="min-width:700px;font-family:Arial,sans-serif">
                """.formatted(width, height));

        // Горизонтальні лінії та шкала температури.
        for (int i = 0; i <= 5; i++) {
            double temperature =
                    minTemperature + temperatureRange * i / 5.0;

            double y =
                    top + plotHeight
                            - plotHeight * i / 5.0;

            svg.append("""
                    <line x1="%d" y1="%.2f"
                          x2="%d" y2="%.2f"
                          stroke="#cccccc"
                          stroke-dasharray="4 4"/>
                    <text x="%d" y="%.2f"
                          text-anchor="end"
                          font-size="12"
                          fill="currentColor">%.1f°C</text>
                    """.formatted(
                    left, y,
                    width - right, y,
                    left - 10, y + 4,
                    temperature
            ));
        }

        // Часова шкала.
        for (int i = 0; i <= 4; i++) {
            long offset =
                    timeRangeMillis * i / 4;

            Instant timestamp =
                    from.plusMillis(offset);

            double x =
                    left + plotWidth * i / 4.0;

            svg.append("""
                    <line x1="%.2f" y1="%d"
                          x2="%.2f" y2="%d"
                          stroke="#cccccc"
                          stroke-dasharray="4 4"/>
                    <text x="%.2f" y="%d"
                          text-anchor="middle"
                          font-size="11"
                          fill="currentColor">%s</text>
                    """.formatted(
                    x, top,
                    x, top + plotHeight,
                    x, top + plotHeight + 20,
                    escapeXml(TIME_FORMAT.format(timestamp))
            ));
        }

        // Лінії датчиків.
        int seriesIndex = 0;

        for (Map.Entry<String, List<TelemetryReading>> entry
                : series.entrySet()) {

            String sensorId = entry.getKey();

            List<TelemetryReading> values =
                    entry.getValue();

            String color =
                    COLORS[seriesIndex % COLORS.length];

            StringBuilder points = new StringBuilder();

            for (TelemetryReading reading : values) {
                Instant timestamp = reading.getReceivedAt();

                if (timestamp == null) {
                    continue;
                }

                double timeOffset =
                        Duration.between(from, timestamp)
                                .toMillis();

                double x =
                        left + plotWidth
                                * timeOffset / timeRangeMillis;

                double temperature =
                        reading.getNumericValue().doubleValue();

                double y =
                        top + plotHeight
                                - plotHeight
                                * (temperature - minTemperature)
                                / temperatureRange;

                points.append(
                        String.format(
                                java.util.Locale.ROOT,
                                "%.2f,%.2f ",
                                x,
                                y
                        )
                );
            }

            if (!points.isEmpty()) {
                svg.append("""
                        <polyline
                            points="%s"
                            fill="none"
                            stroke="%s"
                            stroke-width="2.5"
                            stroke-linejoin="round"
                            stroke-linecap="round">
                            <title>%s</title>
                        </polyline>
                        """.formatted(
                        points,
                        color,
                        escapeXml(sensorId)
                ));
            }

            seriesIndex++;
        }

        // Легенда.
        int legendX = left;
        int legendY = height - 25;

        seriesIndex = 0;

        for (String sensorId : series.keySet()) {
            String color =
                    COLORS[seriesIndex % COLORS.length];

            svg.append("""
                    <circle cx="%d" cy="%d"
                            r="5" fill="%s"/>
                    <text x="%d" y="%d"
                          font-size="12"
                          fill="currentColor">%s</text>
                    """.formatted(
                    legendX,
                    legendY - 4,
                    color,
                    legendX + 10,
                    legendY,
                    escapeXml(sensorId)
            ));

            legendX += Math.max(130, sensorId.length() * 8 + 40);

            if (legendX > width - 150) {
                legendX = left;
                legendY -= 22;
            }

            seriesIndex++;
        }

        svg.append("</svg>");

        return svg.toString();
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

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);

        UI ui = event.getUI();

        ui.setPollInterval(10_000);

        pollRegistration = ui.addPollListener(
                ignored -> refreshChart()
        );
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        event.getUI().setPollInterval(-1);

        super.onDetach(event);
    }
}