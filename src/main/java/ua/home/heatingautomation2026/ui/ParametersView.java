package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ua.home.heatingautomation2026.dto.ParameterDefinitionDto;
import ua.home.heatingautomation2026.dto.ParameterDefinitionRegistrationDto;
import ua.home.heatingautomation2026.enam.ParameterDataType;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.DeviceParameter;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.mapper.ParameterDefinitionMapper;
import ua.home.heatingautomation2026.service.DeviceParameterService;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.ParameterDefinitionService;
import ua.home.heatingautomation2026.service.SensorService;

import java.util.List;

@Route(value = "parameters", layout = MainLayout.class)
@PageTitle("Параметри | Home Heating")
public class ParametersView extends VerticalLayout {

    private final ParameterDefinitionService parameterDefinitionService;
    private final ParameterDefinitionMapper parameterDefinitionMapper;
    private final DeviceService deviceService;
    private final DeviceParameterService deviceParameterService;
    private final SensorService sensorService;

     final Grid<ParameterDefinitionDto> parametersGrid =
            new Grid<>(ParameterDefinitionDto.class, false);

    // Поля створення параметра
    private final TextField codeField = new TextField("Код параметра");
    private final TextField nameField = new TextField("Назва");
    private final TextArea descriptionField = new TextArea("Опис");

    private final ComboBox<ParameterDataType> dataTypeField =
            new ComboBox<>("Тип даних");

    private final TextField unitField = new TextField("Одиниця вимірювання");

    // Поля призначення параметра пристрою
    private final ComboBox<Device> deviceSelect =
            new ComboBox<>("Пристрій");

    private final ComboBox<ParameterDefinitionDto> parameterSelect =
            new ComboBox<>("Параметр");

    private final ComboBox<Device> sensorDeviceSelect =
            new ComboBox<>("Контролер ESP32");

    private final ComboBox<DeviceParameter> sensorAssignmentSelect =
            new ComboBox<>("Призначений параметр");

    private final ComboBox<Sensor> sensorSelect =
            new ComboBox<>("Фізичний датчик");

    public ParametersView(
            ParameterDefinitionService parameterDefinitionService,
            ParameterDefinitionMapper parameterDefinitionMapper,
            DeviceService deviceService,
            DeviceParameterService deviceParameterService,
            SensorService sensorService
    ) {
        this.parameterDefinitionService = parameterDefinitionService;
        this.parameterDefinitionMapper = parameterDefinitionMapper;
        this.deviceService = deviceService;
        this.deviceParameterService = deviceParameterService;
        this.sensorService = sensorService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Параметри");

        configureParametersGrid();
        configureParameterForm();
        configureAssignmentForm();
        configureSensorBindingForm();

        add(
                title,
                new H2("Каталог параметрів"),
                parametersGrid,
                new H2("Створити параметр"),
                createParameterForm(),
                new H2("Призначити параметр пристрою"),
                createAssignmentForm(),
                new H2("Прив'язати фізичний датчик"),
                createSensorBindingForm()
        );

        refreshData();
    }

    private void configureParametersGrid() {

        parametersGrid.addColumn(
                        ParameterDefinitionDto::code
                )
                .setHeader("Код")
                .setAutoWidth(true);

        parametersGrid.addColumn(
                        ParameterDefinitionDto::name
                )
                .setHeader("Назва")
                .setAutoWidth(true)
                .setFlexGrow(1);

        parametersGrid.addColumn(
                        ParameterDefinitionDto::type
                )
                .setHeader("Тип")
                .setAutoWidth(true);

        parametersGrid.addColumn(
                        parameter -> parameter.unit() == null
                                ? "—"
                                : parameter.unit()
                )
                .setHeader("Одиниця")
                .setAutoWidth(true);

        parametersGrid.addColumn(
                        parameter -> parameter.description() == null
                                ? "—"
                                : parameter.description()
                )
                .setHeader("Опис")
                .setAutoWidth(true)
                .setFlexGrow(2);

        parametersGrid.setWidthFull();
        parametersGrid.setHeight("300px");
    }

    private void configureParameterForm() {

        codeField.setPlaceholder("buffer.top_temperature");
        codeField.setHelperText(
                "Унікальний код, наприклад boiler.flue_temperature"
        );

        nameField.setPlaceholder("Температура верхньої частини буфера");

        descriptionField.setMaxLength(500);
        descriptionField.setWidthFull();

        dataTypeField.setItems(ParameterDataType.values());
        dataTypeField.setItemLabelGenerator(Enum::name);
        dataTypeField.setRequired(true);

        unitField.setPlaceholder("°C");
    }

    private FormLayout createParameterForm() {

        FormLayout form = new FormLayout();

        form.add(
                codeField,
                nameField,
                dataTypeField,
                unitField,
                descriptionField
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        Button createButton = new Button(
                "Створити параметр",
                event -> createParameter()
        );

        form.add(createButton);
        form.setColspan(descriptionField, 2);
        form.setColspan(createButton, 2);

        return form;
    }

    private void configureAssignmentForm() {

        deviceSelect.setItemLabelGenerator(
                device -> device.getDeviceName()
                        + " (" + device.getDeviceId() + ")"
        );

        deviceSelect.setWidthFull();

        parameterSelect.setItemLabelGenerator(
                parameter -> parameter.name()
                        + " (" + parameter.code() + ")"
        );

        parameterSelect.setWidthFull();
    }

    private FormLayout createAssignmentForm() {

        FormLayout form = new FormLayout();

        form.add(deviceSelect, parameterSelect);

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        Button assignButton = new Button(
                "Призначити параметр",
                event -> assignParameter()
        );

        form.add(assignButton);
        form.setColspan(assignButton, 2);

        return form;
    }

    private void createParameter() {

        String code = codeField.getValue();
        String name = nameField.getValue();
        ParameterDataType dataType = dataTypeField.getValue();

        if (code == null || code.isBlank()
                || name == null || name.isBlank()
                || dataType == null) {

            Notification.show(
                    "Заповни код, назву та тип параметра",
                    3000,
                    Notification.Position.BOTTOM_START
            );

            return;
        }

        try {
            ParameterDefinitionRegistrationDto parameter = new ParameterDefinitionRegistrationDto(
                    code, name,descriptionField.getValue(), dataType, unitField.getEmptyValue());

            parameterDefinitionService.createParameterDefinition(parameter);

            clearParameterForm();
            refreshData();

            Notification.show(
                    "Параметр успішно створено",
                    3000,
                    Notification.Position.BOTTOM_START
            );

        } catch (RuntimeException ex) {

            Notification.show(
                    "Не вдалося створити параметр: "
                            + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_START
            );
        }
    }

    private void assignParameter() {

        Device device = deviceSelect.getValue();
        ParameterDefinitionDto parameter =
                parameterSelect.getValue();

        if (device == null || parameter == null) {

            Notification.show(
                    "Вибери пристрій і параметр",
                    3000,
                    Notification.Position.BOTTOM_START
            );

            return;
        }

        try {
            deviceParameterService.assignParameter(
                    device.getDeviceId(),
                    parameter.code()
            );

            Notification.show(
                    "Параметр призначено пристрою "
                            + device.getDeviceId(),
                    3000,
                    Notification.Position.BOTTOM_START
            );

        } catch (RuntimeException ex) {

            Notification.show(
                    "Не вдалося призначити параметр: "
                            + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_START
            );
        }
    }

    private void refreshData() {

        List<ParameterDefinitionDto> parameters =
                        parameterDefinitionService.getAllParameterDefinitions().stream().
                                map(parameterDefinitionMapper::toDto)
                                .toList();

        parametersGrid.setItems(parameters);
        parameterSelect.setItems(parameters);

        deviceSelect.setItems(deviceService.findAllDevices());
        sensorDeviceSelect.setItems(deviceService.findAllDevices());
    }

    private void clearParameterForm() {

        codeField.clear();
        nameField.clear();
        descriptionField.clear();
        dataTypeField.clear();
        unitField.clear();
    }

    private void configureSensorBindingForm() {

        sensorDeviceSelect.setItems(deviceService.findAllDevices());

        sensorDeviceSelect.setItemLabelGenerator(
                device -> device.getDeviceName()
                        + " (" + device.getDeviceId() + ")"
        );

        sensorAssignmentSelect.setItemLabelGenerator(
                assignment -> assignment.getParameterDefinition().getName()
                        + " ("
                        + assignment.getParameterDefinition().getCode()
                        + ")"
        );

        sensorSelect.setItemLabelGenerator(
                sensor -> sensor.getSensorName()
                        + " (" + sensor.getSensorId() + ")"
        );

        sensorDeviceSelect.setRequired(true);
        sensorAssignmentSelect.setRequired(true);
        sensorSelect.setRequired(true);

        sensorDeviceSelect.setWidthFull();
        sensorAssignmentSelect.setWidthFull();
        sensorSelect.setWidthFull();

        sensorDeviceSelect.addValueChangeListener(
                event -> refreshSensorOptions()
        );

        refreshSensorOptions();
    }

    private void refreshSensorOptions() {

        Device device = sensorDeviceSelect.getValue();

        sensorAssignmentSelect.clear();
        sensorSelect.clear();

        if (device == null) {
            sensorAssignmentSelect.setItems(List.of());
            sensorSelect.setItems(List.of());
            return;
        }

        List<DeviceParameter> assignments =
                deviceParameterService.getDeviceParameters(
                        device.getDeviceId()
                );

        sensorAssignmentSelect.setItems(assignments);

        List<Sensor> sensors =
                sensorService.findEnabledByDeviceId(
                        device.getDeviceId()
                );

        sensorSelect.setItems(sensors);
    }

    private FormLayout createSensorBindingForm() {

        FormLayout form = new FormLayout();

        form.add(
                sensorDeviceSelect,
                sensorAssignmentSelect,
                sensorSelect
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        Button bindButton = new Button(
                "Прив'язати датчик",
                event -> bindSensor()
        );

        form.add(bindButton);

        return form;
    }

    private void bindSensor() {

        Device device = sensorDeviceSelect.getValue();
        DeviceParameter assignment = sensorAssignmentSelect.getValue();
        Sensor sensor = sensorSelect.getValue();

        if (device == null || assignment == null || sensor == null) {
            Notification.show(
                    "Вибери контролер, параметр і фізичний датчик",
                    3000,
                    Notification.Position.BOTTOM_START
            );
            return;
        }

        try {
            deviceParameterService.assignSensor(
                    device.getDeviceId(),
                    assignment.getParameterDefinition().getCode(),
                    sensor.getSensorId()
            );

            Notification.show(
                    "Датчик успішно прив'язано до параметра",
                    3000,
                    Notification.Position.BOTTOM_START
            );

        } catch (RuntimeException ex) {

            Notification.show(
                    "Не вдалося прив'язати датчик: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_START
            );
        }
    }
}