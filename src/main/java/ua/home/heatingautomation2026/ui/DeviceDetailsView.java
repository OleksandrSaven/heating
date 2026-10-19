package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
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
import com.vaadin.flow.shared.Registration;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.mapper.DeviceParameterMapper;
import ua.home.heatingautomation2026.mapper.TelemetryReadingMapper;
import ua.home.heatingautomation2026.service.DeviceParameterService;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.SensorService;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "device/:deviceId", layout = MainLayout.class)
@PageTitle("Деталі пристрою | Home Heating")
public class DeviceDetailsView extends VerticalLayout
        implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private final DeviceService deviceService;
    private final DeviceParameterService deviceParameterService;
    private final DeviceParameterMapper deviceParameterMapper;
    private final TelemetryService telemetryService;
    private final TelemetryReadingMapper telemetryReadingMapper;
    private final SensorService sensorService;


    private final H1 title = new H1("Деталі пристрою");
    private final Paragraph deviceInfo = new Paragraph();
    private final Span deviceStatus = new Span();
    private final Span lastHeartbeat = new Span();

    private final Grid<Sensor> parametersGrid =
            new Grid<>(Sensor.class, false);

    private final Grid<TelemetryReadingResponse> currentReadingsGrid =
            new Grid<>(TelemetryReadingResponse.class, false);

    private final Grid<TelemetryReadingResponse> historyGrid =
            new Grid<>(TelemetryReadingResponse.class, false);

    private String currentDeviceId;
    private Registration pollRegistration;

    public DeviceDetailsView(
            DeviceService deviceService,
            DeviceParameterService deviceParameterService,
            DeviceParameterMapper deviceParameterMapper,
            TelemetryService telemetryService,
            TelemetryReadingMapper telemetryReadingMapper, SensorService sensorService
    ) {
        this.deviceService = deviceService;
        this.deviceParameterService = deviceParameterService;
        this.deviceParameterMapper = deviceParameterMapper;
        this.telemetryService = telemetryService;
        this.telemetryReadingMapper = telemetryReadingMapper;
        this.sensorService = sensorService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);
        addClassName("page-view");

        Button backButton = new Button("← До пристроїв");
        backButton.addClickListener(event ->
                backButton.getUI().ifPresent(ui ->
                        ui.navigate("devices")
                )
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

        HorizontalLayout deviceStatusLayout =
                new HorizontalLayout(deviceStatus, lastHeartbeat);

        deviceStatusLayout.setWidthFull();
        deviceStatusLayout.setSpacing(true);

        configureParametersGrid();
        configureCurrentReadingsGrid();
        configureHistoryGrid();

        add(
                header,
                deviceInfo,
                deviceStatusLayout,
                new H2("Датчики"),
                parametersGrid,
                new H2("Поточні значення"),
                currentReadingsGrid,
                new H2("Історія телеметрії"),
                historyGrid
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentDeviceId = event.getRouteParameters()
                .get("deviceId")
                .orElse(null);

        refreshData();
    }

    private void configureParametersGrid() {
        parametersGrid.addClassName("responsive-grid");
        parametersGrid.addColumn(
                        Sensor::getSensorId
                )
                .setHeader("Id датчика")
                .setAutoWidth(true);

        parametersGrid.addColumn(
                        Sensor::getSensorName
                )
                .setHeader("Назва")
                .setAutoWidth(true)
                .setFlexGrow(1);

        parametersGrid.addColumn(
                        parameter -> parameter.getParameterDefinition().getName()
                )
                .setHeader("Тип")
                .setAutoWidth(true);

        parametersGrid.addColumn(
                        parameter -> parameter.getParameterDefinition() == null
                                ? "—"
                                : parameter.getParameterDefinition().getUnit()
                )
                .setHeader("Одиниця")
                .setAutoWidth(true);

        parametersGrid.addColumn(
                        parameter -> parameter.isEnabled()
                                ? "Увімкнений"
                                : "Вимкнений"
                )
                .setHeader("Доступність")
                .setAutoWidth(true);


/*
        parametersGrid.addColumn(
                        parameter -> parameter.sensorName() == null
                                ? "Не прив'язано"
                                : parameter.sensorName()
                )
                .setHeader("Фізичний датчик")
                .setAutoWidth(true)
                .setFlexGrow(1);

        parametersGrid.addColumn(
                        parameter -> parameter.sensorId() == null
                                ? "—"
                                : parameter.sensorId()
                )
                .setHeader("ID датчика")
                .setAutoWidth(true);

        parametersGrid.addColumn(
                        parameter -> parameter.sensorType() == null
                                ? "—"
                                : parameter.sensorType()
                )
                .setHeader("Тип датчика")
                .setAutoWidth(true);
*/

        parametersGrid.setWidthFull();
        parametersGrid.setHeight("250px");
    }

    private void configureCurrentReadingsGrid() {
        currentReadingsGrid.addClassName("responsive-grid");
        currentReadingsGrid.addColumn(
                        TelemetryReadingResponse::parameterName
                )
                .setHeader("Параметр")
                .setAutoWidth(true)
                .setFlexGrow(1);

        currentReadingsGrid.addColumn(
                        this::formatValue
                )
                .setHeader("Значення")
                .setAutoWidth(true);

        currentReadingsGrid.addColumn(
                        reading -> reading.unit() == null
                                ? "—"
                                : reading.unit()
                )
                .setHeader("Одиниця")
                .setAutoWidth(true);

        currentReadingsGrid.addColumn(
                        this::getReadingTimestamp
                )
                .setHeader("Час")
                .setAutoWidth(true);

        currentReadingsGrid.setWidthFull();
        currentReadingsGrid.setHeight("250px");
    }

    private void configureHistoryGrid() {
        historyGrid.addClassName("responsive-grid");
        historyGrid.addColumn(
                        TelemetryReadingResponse::parameterName
                )
                .setHeader("Параметр")
                .setAutoWidth(true)
                .setFlexGrow(1);

        historyGrid.addColumn(
                        TelemetryReadingResponse::parameterCode
                )
                .setHeader("Код")
                .setAutoWidth(true);

        historyGrid.addColumn(
                        this::formatValue
                )
                .setHeader("Значення")
                .setAutoWidth(true);

        historyGrid.addColumn(
                        reading -> reading.unit() == null
                                ? "—"
                                : reading.unit()
                )
                .setHeader("Одиниця")
                .setAutoWidth(true);

        historyGrid.addColumn(
                        this::getReadingTimestamp
                )
                .setHeader("Час отримання")
                .setAutoWidth(true);

        historyGrid.addColumn(
                        reading -> reading.sensorName() == null
                                ? "Не зафіксовано"
                                : reading.sensorName()
                )
                .setHeader("Фізичний датчик")
                .setAutoWidth(true);

        historyGrid.setWidthFull();
        historyGrid.setHeight("350px");
    }

    private void refreshData() {

        if (currentDeviceId == null || currentDeviceId.isBlank()) {
            return;
        }

        try {
            Device device =
                    deviceService.findDevice(currentDeviceId);

            title.setText(device.getDeviceName());

            deviceInfo.setText(
                    "ID: " + device.getDeviceId()
                            + " | Тип: "
                            + device.getDeviceType()
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

            // Завантажуємо призначені параметри
            List<Sensor> parameters = sensorService.findAllByDeviceId(device.getDeviceId());

            parametersGrid.setItems(parameters);

            // Завантажуємо останні 100 записів телеметрії
            List<TelemetryReadingResponse> readings = telemetryService.getTelemetryReadings(currentDeviceId);

            // Історія містить усі отримані записи з вибірки
            historyGrid.setItems(readings);

            // Для кожного параметра залишаємо лише найновіший запис.
            // Репозиторій повертає записи від найновішого до найстарішого.
            Map<String, TelemetryReadingResponse> latestByParameter =
                    new LinkedHashMap<>();

            for (TelemetryReadingResponse reading : readings) {
                latestByParameter.putIfAbsent(
                        reading.parameterCode(),
                        reading
                );
            }

            currentReadingsGrid.setItems(
                    new ArrayList<>(latestByParameter.values())
            );

        } catch (EntityNotFoundException ex) {

            title.setText("Пристрій не знайдено");
            deviceInfo.setText("ID: " + currentDeviceId);
            deviceStatus.setText("Перевір ідентифікатор пристрою.");
            lastHeartbeat.setText("");

            parametersGrid.setItems(List.of());
            currentReadingsGrid.setItems(List.of());
            historyGrid.setItems(List.of());
        }
    }

    private String formatValue(TelemetryReadingResponse reading) {

        if (reading.numericValue() != null) {
            return reading.numericValue()
                    .stripTrailingZeros()
                    .toPlainString();
        }

        if (reading.booleanValue() != null) {
            return reading.booleanValue() ? "Так" : "Ні";
        }

        if (reading.textValue() != null) {
            return reading.textValue();
        }

        return "—";
    }

    private String getReadingTimestamp(
            TelemetryReadingResponse reading
    ) {
        Instant timestamp = reading.measuredAt() != null
                ? reading.measuredAt()
                : reading.receivedAt();

        return formatTimestamp(timestamp);
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