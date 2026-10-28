 package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.SensorService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "device/:deviceId", layout = MainLayout.class)
@PageTitle("Деталі пристрою | Home Heating")
public class DeviceDetailsView extends VerticalLayout
        implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private final DeviceService deviceService;
    private final SensorService sensorService;

    private final H1 title = new H1("Деталі пристрою");

    private final Button accumulatorButton = new Button(
            "🔥 Теплоакумулятор",
            event -> getUI().ifPresent(
                    ui -> ui.navigate("accumulator")
            )
    );

    private final Paragraph deviceInfo = new Paragraph();
    private final Span deviceStatus = new Span();
    private final Span lastHeartbeat = new Span();

    private final Div deviceInfoCard = new Div();
    private final Div sensorsSection = new Div();
    private final Div sensorCardsContainer = new Div();

    private final Grid<Sensor> sensorsGrid =
            new Grid<>(Sensor.class, false);

    private String currentDeviceId;

    public DeviceDetailsView(
            DeviceService deviceService,
            SensorService sensorService
    ) {
        this.deviceService = deviceService;
        this.sensorService = sensorService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        addClassName("device-details-view");

        // Верхній рядок
        Button backButton = new Button(
                "← До пристроїв",
                event -> getUI().ifPresent(
                        ui -> ui.navigate("")
                )
        );
        backButton.addClassName("device-back-button");

        title.addClassName("device-page-title");

        HorizontalLayout titleRow =
                new HorizontalLayout(backButton, title);

        titleRow.addClassName("device-title-row");
        titleRow.setWidthFull();
        titleRow.setAlignItems(Alignment.CENTER);

        // Кнопки дій
        Button refreshButton = new Button(
                "Оновити",
                VaadinIcon.REFRESH.create(),
                event -> refreshData()
        );
        refreshButton.addClassName("device-action-button");

        accumulatorButton.addClassName("device-action-button");

        HorizontalLayout actionsRow =
                new HorizontalLayout(accumulatorButton, refreshButton);

        actionsRow.addClassName("device-actions-row");
        actionsRow.setWidthFull();
        actionsRow.setAlignItems(Alignment.CENTER);

        // Шапка
        VerticalLayout header = new VerticalLayout(
                titleRow,
                actionsRow
        );

        header.addClassName("device-page-header");
        header.setPadding(false);
        header.setSpacing(true);
        header.setWidthFull();

        // Інформація про пристрій
        deviceInfoCard.addClassName("device-info-card");

        H3 infoTitle = new H3("Інформація про пристрій");

        deviceInfo.addClassName("device-info-text");
        deviceStatus.addClassName("device-status-text");
        lastHeartbeat.addClassName("device-heartbeat-text");

        VerticalLayout infoContent = new VerticalLayout(
                infoTitle,
                deviceInfo,
                deviceStatus,
                lastHeartbeat
        );

        infoContent.setPadding(false);
        infoContent.setSpacing(true);

        deviceInfoCard.add(infoContent);

        // Датчики
        H3 sensorsTitle = new H3("Датчики");
        sensorsTitle.addClassName("sensors-section-title");

        sensorsGrid.addClassName("responsive-grid");

        configureSensorsGrid();

        sensorCardsContainer.addClassName("sensor-cards-container");

        sensorsSection.addClassName("sensors-section");
        sensorsSection.add(sensorsTitle, sensorsGrid, sensorCardsContainer);

        add(header, deviceInfoCard, sensorsSection);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentDeviceId = event.getRouteParameters()
                .get("deviceId")
                .orElse(null);

        refreshData();
    }

    private void configureSensorsGrid() {
        sensorsGrid.addColumn(Sensor::getSensorId)
                .setHeader("ID датчика")
                .setAutoWidth(true);

        sensorsGrid.addColumn(Sensor::getSensorName)
                .setHeader("Назва датчика")
                .setAutoWidth(true)
                .setFlexGrow(1);

        sensorsGrid.addColumn(sensor ->
                        sensor.getParameterDefinition() == null
                                ? "—"
                                : sensor.getParameterDefinition().getName()
                )
                .setHeader("Параметр")
                .setAutoWidth(true);

        sensorsGrid.addColumn(sensor ->
                        sensor.getParameterDefinition() == null
                                || sensor.getParameterDefinition().getUnit() == null
                                ? "—"
                                : sensor.getParameterDefinition().getUnit()
                )
                .setHeader("Одиниця")
                .setAutoWidth(true);

        sensorsGrid.addColumn(sensor ->
                        sensor.isEnabled()
                                ? "Увімкнений"
                                : "Вимкнений"
                )
                .setHeader("Статус")
                .setAutoWidth(true);

        sensorsGrid.setWidthFull();
        sensorsGrid.setHeight("350px");

        sensorsGrid.getStyle().set("cursor", "pointer");

        sensorsGrid.addItemClickListener(event ->
                navigateToSensorChart(event.getItem())
        );
    }

    private Div createSensorCard(Sensor sensor) {
        Div card = new Div();
        card.addClassName("sensor-mobile-card");

        H3 sensorName = new H3(sensor.getSensorName());
        sensorName.addClassName("sensor-mobile-name");

        Paragraph sensorId = new Paragraph(
                "ID: " + sensor.getSensorId()
        );

        String parameter = sensor.getParameterDefinition() == null
                ? "—"
                : sensor.getParameterDefinition().getName();

        String unit = sensor.getParameterDefinition() == null
                || sensor.getParameterDefinition().getUnit() == null
                ? "—"
                : sensor.getParameterDefinition().getUnit();

        Paragraph parameterInfo = new Paragraph(
                "Параметр: " + parameter
        );

        Paragraph unitInfo = new Paragraph(
                "Одиниця вимірювання: " + unit
        );

        Span status = new Span(
                sensor.isEnabled() ? "● Увімкнений" : "● Вимкнений"
        );

        status.addClassName(
                sensor.isEnabled()
                        ? "sensor-status-enabled"
                        : "sensor-status-disabled"
        );

        Span openHint = new Span("Переглянути графік →");
        openHint.addClassName("sensor-open-hint");

        card.add(
                sensorName,
                sensorId,
                parameterInfo,
                unitInfo,
                status,
                openHint
        );

        card.getStyle().set("cursor", "pointer");

        card.addClickListener(event -> navigateToSensorChart(sensor));

        return card;
    }

    private void navigateToSensorChart(Sensor sensor) {
        getUI().ifPresent(ui ->
                ui.navigate("telemetry-chart/" + sensor.getSensorId())
        );
    }

    private void refreshData() {
        if (currentDeviceId == null || currentDeviceId.isBlank()) {
            return;
        }

        try {
            Device device = deviceService.findDevice(currentDeviceId);

            title.setText(device.getDeviceName());

            accumulatorButton.setVisible(
                    device.getDeviceName() != null
                            && "LIGHT TA 1000".equalsIgnoreCase(
                            device.getDeviceName().trim()
                    )
            );

            deviceInfo.setText(
                    "ID: " + device.getDeviceId()
                            + " | Тип: " + device.getDeviceType()
            );

            deviceStatus.setText(
                    "Статус: " + getStatusLabel(
                            device.getDeviceRegistrationStatus().name()
                    )
            );

            lastHeartbeat.setText(
                    "Останній зв'язок: "
                            + formatTimestamp(device.getLastHeartbeat())
            );

            List<Sensor> sensors =
                    sensorService.findAllByDeviceId(device.getDeviceId());

            // Таблиця для комп'ютера
            sensorsGrid.setItems(sensors);

            // Картки для телефону
            sensorCardsContainer.removeAll();

            if (sensors.isEmpty()) {
                Paragraph emptyMessage =
                        new Paragraph("У цього пристрою немає датчиків.");

                emptyMessage.addClassName("sensors-empty-message");

                sensorCardsContainer.add(emptyMessage);
            } else {
                sensors.stream()
                        .map(this::createSensorCard)
                        .forEach(sensorCardsContainer::add);
            }

        } catch (EntityNotFoundException ex) {
            title.setText("Пристрій не знайдено");
            deviceInfo.setText("ID: " + currentDeviceId);
            deviceStatus.setText("Перевір ідентифікатор пристрою.");
            lastHeartbeat.setText("");

            sensorsGrid.setItems(List.of());
            sensorCardsContainer.removeAll();

            Paragraph errorMessage =
                    new Paragraph("Не вдалося знайти пристрої цього датчика.");

            sensorCardsContainer.add(errorMessage);
        }
    }

    private String formatTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return "Немає даних";
        }

        return DATE_FORMAT.format(timestamp);
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "Очікує підтвердження";
            case "ACTIVE" -> "Активний";
            case "DISABLED" -> "Вимкнений";
            default -> status;
        };
    }
}
