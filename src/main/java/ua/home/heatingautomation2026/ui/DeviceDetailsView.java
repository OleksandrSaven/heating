 package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(value = "device/:deviceId", layout = MainLayout.class)
@PageTitle("Деталі пристрою | Home Heating")
public class DeviceDetailsView extends VerticalLayout
        implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private final DeviceService deviceService;
    private final SensorService sensorService;

    private final H1 title = new H1("Деталі пристрою");

    private final Button accumulatorButton = new Button(
            "Котельня",
            VaadinIcon.FIRE.create(),
            event -> getUI().ifPresent(ui -> ui.navigate("boiler-room"))
    );

    private final Paragraph deviceInfo = new Paragraph();
    private final Span deviceStatus = new Span();
    private final Span lastHeartbeat = new Span();

    private final Div deviceInfoCard = new Div();
    private final Div sensorGroupsContainer = new Div();

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

        // Кнопка повернення
        Button backButton = new Button(
                "До пристроїв",
                VaadinIcon.ARROW_LEFT.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("devices"))
        );
        backButton.addClassName("device-back-button");

        title.addClassName("device-page-title");

        HorizontalLayout titleRow = new HorizontalLayout(backButton, title);
        titleRow.addClassName("device-title-row");
        titleRow.setWidthFull();
        titleRow.setAlignItems(Alignment.CENTER);
        titleRow.expand(title);

        // Дії
        Button refreshButton = new Button(
                "Оновити",
                VaadinIcon.REFRESH.create(),
                event -> refreshData()
        );
        refreshButton.addClassName("device-action-button");

        accumulatorButton.addClassName("device-accumulator-button");
        accumulatorButton.setVisible(false);

        HorizontalLayout actionsRow =
                new HorizontalLayout(accumulatorButton, refreshButton);

        actionsRow.addClassName("device-actions-row");
        actionsRow.setWidthFull();
        actionsRow.setAlignItems(Alignment.CENTER);

        VerticalLayout header = new VerticalLayout(titleRow, actionsRow);
        header.addClassName("device-page-header");
        header.setPadding(false);
        header.setSpacing(true);
        header.setWidthFull();

        // Картка інформації про контролер
        deviceInfoCard.addClassName("device-info-card");

        H2 infoTitle = new H2("Інформація про пристрій");
        infoTitle.addClassName("device-section-title");

        deviceInfo.addClassName("device-info-text");
        deviceStatus.addClassName("device-status-text");
        lastHeartbeat.addClassName("device-heartbeat-text");

        VerticalLayout infoContent = new VerticalLayout(
                infoTitle,
                deviceInfo,
                deviceStatus,
                lastHeartbeat
        );

        infoContent.addClassName("device-info-content");
        infoContent.setPadding(false);
        infoContent.setSpacing(true);

        deviceInfoCard.add(infoContent);

        // Категорії параметрів і картки датчиків
        Div sensorsSection = new Div();
        sensorsSection.addClassName("device-sensors-section");

        H2 sensorsTitle = new H2("Параметри та датчики");
        sensorsTitle.addClassName("device-section-title");

        Paragraph sensorsDescription = new Paragraph(
                "Параметри згруповані за типом вимірювання. "
                        + "Виберіть картку датчика, щоб відкрити графік."
        );
        sensorsDescription.addClassName("device-section-description");

        sensorGroupsContainer.addClassName("device-sensor-groups");

        sensorsSection.add(
                sensorsTitle,
                sensorsDescription,
                sensorGroupsContainer
        );

        add(header, deviceInfoCard, sensorsSection);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentDeviceId = event.getRouteParameters()
                .get("deviceId")
                .orElse(null);

        refreshData();
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
                            && "BOILER ROOM 01".equalsIgnoreCase(
                            device.getDeviceName().trim()
                    )
            );

            deviceInfo.setText(
                    "ID контролера: " + device.getDeviceId()
                            + " | Тип: "
                            + (device.getDeviceType() == null
                            ? "—"
                            : device.getDeviceType().name())
            );

            deviceStatus.setText(
                    "Статус реєстрації: "
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

            renderSensorGroups(sensors);

        } catch (EntityNotFoundException ex) {
            title.setText("Пристрій не знайдено");
            deviceInfo.setText("ID: " + currentDeviceId);
            deviceStatus.setText("Перевірте ідентифікатор пристрою.");
            lastHeartbeat.setText("");

            sensorGroupsContainer.removeAll();

            Paragraph errorMessage = new Paragraph(
                    "Не вдалося знайти пристрій або його датчики."
            );
            errorMessage.addClassName("device-empty-message");

            sensorGroupsContainer.add(errorMessage);

        } catch (RuntimeException ex) {
            Paragraph errorMessage = new Paragraph(
                    "Помилка завантаження даних: " + ex.getMessage()
            );
            errorMessage.addClassName("device-error-message");

            sensorGroupsContainer.removeAll();
            sensorGroupsContainer.add(errorMessage);
        }
    }

    private void renderSensorGroups(List<Sensor> sensors) {
        sensorGroupsContainer.removeAll();

        if (sensors == null || sensors.isEmpty()) {
            Paragraph emptyMessage = new Paragraph(
                    "У цього пристрою ще немає зареєстрованих датчиків."
            );
            emptyMessage.addClassName("device-empty-message");

            sensorGroupsContainer.add(emptyMessage);
            return;
        }

        Map<String, List<Sensor>> groups = new LinkedHashMap<>();

        for (Sensor sensor : sensors) {
            String category = getParameterCategory(sensor);

            groups.computeIfAbsent(category, key ->
                    new java.util.ArrayList<>()
            ).add(sensor);
        }

        for (Map.Entry<String, List<Sensor>> entry : groups.entrySet()) {
            Div groupCard = new Div();
            groupCard.addClassName("device-parameter-group");

            Div groupHeader = new Div();
            groupHeader.addClassName("device-parameter-group-header");

            H3 groupTitle = new H3(entry.getKey());
            groupTitle.addClassName("device-parameter-group-title");

            Span count = new Span(
                    String.valueOf(entry.getValue().size())
            );
            count.addClassName("device-parameter-count");

            groupHeader.add(groupTitle, count);

            Div cards = new Div();
            cards.addClassName("device-sensor-cards");

            for (Sensor sensor : entry.getValue()) {
                cards.add(createSensorCard(sensor));
            }

            groupCard.add(groupHeader, cards);
            sensorGroupsContainer.add(groupCard);
        }
    }


    private Div createSensorCard(Sensor sensor) {
        Div card = new Div();
        card.addClassName("device-sensor-card");

        // Назва датчика
        String sensorName = sensor.getSensorName() == null
                || sensor.getSensorName().isBlank()
                ? sensor.getSensorId()
                : sensor.getSensorName();

        H3 name = new H3(sensorName);
        name.addClassName("device-sensor-name");

        // ID датчика
        Span sensorId = new Span("ID: " + sensor.getSensorId());
        sensorId.addClassName("device-sensor-id");

        // Статус
        Span status = new Span(
                sensor.isEnabled() ? "● Увімкнений" : "● Вимкнений"
        );

        status.addClassName("device-sensor-status");
        status.addClassName(
                sensor.isEnabled()
                        ? "device-sensor-status-enabled"
                        : "device-sensor-status-disabled"
        );

        // Рядок з ID та статусом
        Div metaRow = new Div(sensorId, status);
        metaRow.addClassName("device-sensor-meta");

        // Підказка переходу
        Span openHint = new Span("Переглянути графік →");
        openHint.addClassName("device-sensor-open-hint");

        card.add(name, metaRow, openHint);

        card.getElement().setAttribute("role", "button");
        card.getElement().setAttribute("tabindex", "0");
        card.getElement().setAttribute("aria-label",
                "Відкрити графік датчика " + sensorName);

        card.addClickListener(event -> navigateToSensorChart(sensor));

        return card;
    }

    /**
     * Визначає категорію за назвою, кодом та одиницею параметра.
     * Якщо назва параметра не відповідає відомим категоріям,
     * датчик потрапляє до "Інші параметри".
     */
    private String getParameterCategory(Sensor sensor) {
        if (sensor.getParameterDefinition() == null) {
            return "Інші параметри";
        }

        String name = sensor.getParameterDefinition().getName();
        String unit = sensor.getParameterDefinition().getUnit();
        String code = sensor.getParameterDefinition().getCode();

        String text = (
                (name == null ? "" : name) + " "
                        + (unit == null ? "" : unit) + " "
                        + (code == null ? "" : code)
        ).toLowerCase(Locale.ROOT);

        if (containsAny(text,
                "температур", "temperature", "temp",
                "°c", "celsius", "димохід", "теплоакумулятор")) {
            return "🌡 Температура";
        }

        if (containsAny(text,
                "тиск", "pressure", "bar", "кпа", "kpa", "mpa")) {
            return "💧 Тиск";
        }

        if (containsAny(text,
                "напруг", "voltage", "volt", "струм", "current",
                "ампер", "ampere", "потужн", "power", "ват", "watt",
                "енергі", "energy", "квт")) {
            return "⚡ Електричні параметри";
        }

        if (containsAny(text,
                "волог", "humidity", "hum")) {
            return "💧 Вологість";
        }

        if (containsAny(text,
                "швидкіст", "speed", "витрат", "flow", "volume",
                "об'єм", "об’єм")) {
            return "📈 Витрата та швидкість";
        }

        return "📊 Інші параметри";
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private void navigateToSensorChart(Sensor sensor) {
        getUI().ifPresent(ui ->
                ui.navigate("sensor/" + sensor.getSensorId())
        );
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
