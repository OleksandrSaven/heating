package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.SensorService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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

    private final Grid<Sensor> sensorsGrid =
            new Grid<>(Sensor.class, false);

    private String currentDeviceId;
    private Registration pollRegistration;

    public DeviceDetailsView(
            DeviceService deviceService,
            SensorService sensorService
    ) {
        this.deviceService = deviceService;
        this.sensorService = sensorService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);
        addClassName("page-view");

        Button backButton = new Button(
                "← До пристроїв",
                event -> getUI().ifPresent(
                        ui -> ui.navigate("devices")
                )
        );

        Button refreshButton = new Button(
                "Оновити",
                event -> refreshData()
        );

        HorizontalLayout header =
                new HorizontalLayout(
                        backButton,
                        title,
                        accumulatorButton,
                        refreshButton
                );

        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);

        HorizontalLayout statusLayout =
                new HorizontalLayout(deviceStatus, lastHeartbeat);

        statusLayout.setWidthFull();
        statusLayout.setAlignItems(Alignment.CENTER);

        configureSensorsGrid();

        add(
                header,
                deviceInfo,
                statusLayout,
                sensorsGrid
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentDeviceId = event.getRouteParameters()
                .get("deviceId")
                .orElse(null);

        refreshData();
    }

    private void configureSensorsGrid() {
        sensorsGrid.addClassName("responsive-grid");

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

        sensorsGrid.addItemClickListener(event -> {
            Sensor sensor = event.getItem();

            getUI().ifPresent(ui ->
                    ui.navigate(
                            "telemetry-chart/" + sensor.getSensorId()
                    )
            );
        });
    }

    private void refreshData() {
        if (currentDeviceId == null || currentDeviceId.isBlank()) {
            return;
        }

        try {
            Device device =
                    deviceService.findDevice(currentDeviceId);
            title.setText(device.getDeviceName());

            accumulatorButton.setVisible(
                    "LIGHT TA 1000".equalsIgnoreCase(
                            device.getDeviceName().trim()
                    )
            );
            deviceInfo.setText(
                    "ID: " + device.getDeviceId()
                            + " | Тип: " + device.getDeviceType()
            );

            deviceStatus.setText(
                    "Статус: "
                            + getStatusLabel(
                            device.getDeviceRegistrationStatus().name()
                    )
            );

            lastHeartbeat.setText(
                    "Останній зв'язок: "
                            + formatTimestamp(device.getLastHeartbeat())
            );

            List<Sensor> sensors =
                    sensorService.findAllByDeviceId(device.getDeviceId());

            sensorsGrid.setItems(sensors);

        } catch (EntityNotFoundException ex) {
            title.setText("Пристрій не знайдено");
            deviceInfo.setText("ID: " + currentDeviceId);
            deviceStatus.setText("Перевір ідентифікатор пристрою.");
            lastHeartbeat.setText("");
            sensorsGrid.setItems(List.of());
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();
        ui.setPollInterval(5_000);

        pollRegistration = ui.addPollListener(
                event -> refreshData()
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        detachEvent.getUI().setPollInterval(-1);

        super.onDetach(detachEvent);
    }
}