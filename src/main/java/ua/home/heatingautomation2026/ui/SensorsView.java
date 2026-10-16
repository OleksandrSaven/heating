package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.SensorService;

import java.util.List;

@Route(value = "sensors", layout = MainLayout.class)
@PageTitle("Датчики | Home Heating")
public class SensorsView extends VerticalLayout {

    private final SensorService sensorService;
    private final DeviceService deviceService;

    private final Grid<Sensor> sensorsGrid =
            new Grid<>(Sensor.class, false);

    private final TextField sensorIdField =
            new TextField("ID датчика");

    private final TextField sensorNameField =
            new TextField("Назва датчика");

    private final TextField sensorTypeField =
            new TextField("Тип датчика");

    private final ComboBox<Device> deviceSelect =
            new ComboBox<>("Контролер");

    public SensorsView(
            SensorService sensorService,
            DeviceService deviceService
    ) {
        this.sensorService = sensorService;
        this.deviceService = deviceService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Фізичні датчики");

        Paragraph description = new Paragraph(
                "Кожен фізичний датчик реєструється окремо "
                        + "та належить одному контролеру."
        );

        configureGrid();
        configureForm();

        Button refreshButton = new Button(
                "Оновити",
                event -> refreshData()
        );

        HorizontalLayout header =
                new HorizontalLayout(title, refreshButton);

        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);

        add(
                header,
                description,
                new H2("Зареєстровані датчики"),
                sensorsGrid,
                new H2("Зареєструвати датчик"),
                createForm()
        );

        refreshData();
    }

    private void configureGrid() {

        sensorsGrid.addColumn(Sensor::getSensorId)
                .setHeader("ID датчика")
                .setAutoWidth(true);

        sensorsGrid.addColumn(Sensor::getSensorName)
                .setHeader("Назва")
                .setAutoWidth(true)
                .setFlexGrow(1);

        sensorsGrid.addColumn(Sensor::getSensorType)
                .setHeader("Тип")
                .setAutoWidth(true);

        sensorsGrid.addColumn(
                        sensor -> sensor.getDevice().getDeviceName()
                )
                .setHeader("Контролер")
                .setAutoWidth(true)
                .setFlexGrow(1);

        sensorsGrid.addColumn(
                        sensor -> sensor.getDevice().getDeviceId()
                )
                .setHeader("ID контролера")
                .setAutoWidth(true);

        sensorsGrid.addColumn(
                        sensor -> sensor.isEnabled()
                                ? "Активний"
                                : "Вимкнений"
                )
                .setHeader("Стан")
                .setAutoWidth(true);

        sensorsGrid.addComponentColumn(this::createActions)
                .setHeader("Дії")
                .setAutoWidth(true);

        sensorsGrid.setWidthFull();
        sensorsGrid.setHeight("400px");
    }

    private void configureForm() {

        sensorIdField.setPlaceholder("sensor-flue-01");
        sensorIdField.setRequired(true);
        sensorIdField.setMaxLength(100);

        sensorNameField.setPlaceholder(
                "Датчик температури димових газів"
        );
        sensorNameField.setRequired(true);
        sensorNameField.setMaxLength(150);

        sensorTypeField.setPlaceholder("DS18B20");
        sensorTypeField.setRequired(true);
        sensorTypeField.setMaxLength(50);

        deviceSelect.setItems(deviceService.findAllDevices());

        deviceSelect.setItemLabelGenerator(
                device -> device.getDeviceName()
                        + " (" + device.getDeviceId() + ")"
        );

        deviceSelect.setRequired(true);
        deviceSelect.setWidthFull();
    }

    private FormLayout createForm() {

        FormLayout form = new FormLayout();

        form.add(
                sensorIdField,
                sensorNameField,
                sensorTypeField,
                deviceSelect
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        Button createButton = new Button(
                "Зареєструвати датчик",
                event -> registerSensor()
        );

        form.add(createButton);
        form.setColspan(createButton, 2);

        return form;
    }

    private HorizontalLayout createActions(Sensor sensor) {

        Button toggleButton = new Button(
                sensor.isEnabled() ? "Вимкнути" : "Увімкнути"
        );

        toggleButton.addClickListener(event -> {

            try {
                sensorService.setEnabled(
                        sensor.getId(),
                        !sensor.isEnabled()
                );

                refreshData();

                Notification.show(
                        sensor.isEnabled()
                                ? "Датчик активовано"
                                : "Датчик вимкнено",
                        3000,
                        Notification.Position.BOTTOM_START
                );

            } catch (RuntimeException ex) {

                Notification.show(
                        "Не вдалося змінити стан: " + ex.getMessage(),
                        5000,
                        Notification.Position.BOTTOM_START
                );
            }
        });

        return new HorizontalLayout(toggleButton);
    }

    private void registerSensor() {

        if (sensorIdField.isEmpty()
                || sensorNameField.isEmpty()
                || sensorTypeField.isEmpty()
                || deviceSelect.isEmpty()) {

            Notification.show(
                    "Заповни всі поля та вибери контролер",
                    3000,
                    Notification.Position.BOTTOM_START
            );

            return;
        }

        Device device = deviceSelect.getValue();

        try {
            sensorService.registerSensor(
                    sensorIdField.getValue(),
                    sensorNameField.getValue(),
                    sensorTypeField.getValue(),
                    device.getDeviceId()
            );

            sensorIdField.clear();
            sensorNameField.clear();
            sensorTypeField.clear();

            refreshData();

            Notification.show(
                    "Датчик успішно зареєстровано",
                    3000,
                    Notification.Position.BOTTOM_START
            );

        } catch (RuntimeException ex) {

            Notification.show(
                    "Не вдалося зареєструвати датчик: "
                            + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_START
            );
        }
    }

    private void refreshData() {

        List<Sensor> sensors = sensorService.findAll();

        sensorsGrid.setItems(sensors);

        deviceSelect.setItems(deviceService.findAllDevices());
    }
}