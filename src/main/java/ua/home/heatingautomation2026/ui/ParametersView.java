package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ua.home.heatingautomation2026.dto.ParameterDefinitionDto;
import ua.home.heatingautomation2026.dto.ParameterDefinitionRegistrationDto;
import ua.home.heatingautomation2026.enam.ParameterDataType;
import ua.home.heatingautomation2026.mapper.ParameterDefinitionMapper;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.ParameterDefinitionService;

import java.util.List;

@Route(value = "parameters", layout = MainLayout.class)
@PageTitle("Параметри | Home Heating")
public class ParametersView extends VerticalLayout {

    private final ParameterDefinitionService parameterDefinitionService;
    private final ParameterDefinitionMapper parameterDefinitionMapper;
    private final DeviceService deviceService;

    private final Grid<ParameterDefinitionDto> parametersGrid =
            new Grid<>(ParameterDefinitionDto.class, false);

    private final TextField codeField = new TextField("Код параметра");
    private final TextField nameField = new TextField("Назва");
    private final TextArea descriptionField = new TextArea("Опис");

    private final ComboBox<ParameterDataType> dataTypeField =
            new ComboBox<>("Тип даних");

    private final TextField unitField = new TextField("Одиниця вимірювання");

    // Поля призначення параметра пристрою
    private final ComboBox<ua.home.heatingautomation2026.entity.Device> deviceSelect =
            new ComboBox<>("Пристрій");

    private final ComboBox<ParameterDefinitionDto> parameterSelect =
            new ComboBox<>("Параметр");

    private final ComboBox<ua.home.heatingautomation2026.entity.Device> sensorDeviceSelect =
            new ComboBox<>("Контролер ESP32");

    private final Div feedbackContainer = new Div();

    public ParametersView(
            ParameterDefinitionService parameterDefinitionService,
            ParameterDefinitionMapper parameterDefinitionMapper,
            DeviceService deviceService
    ) {
        this.parameterDefinitionService = parameterDefinitionService;
        this.parameterDefinitionMapper = parameterDefinitionMapper;
        this.deviceService = deviceService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        addClassName("parameters-view");

        H1 title = new H1("Параметри");
        title.addClassName("parameters-title");

        Paragraph subtitle = new Paragraph(
                "Керування каталогом параметрів системи опалення."
        );
        subtitle.addClassName("parameters-subtitle");

        configureParametersGrid();
        configureParameterForm();
        configureAssignmentForm();

        Div catalogCard = new Div();
        catalogCard.addClassName("parameters-card");

        H2 catalogTitle = new H2("Каталог параметрів");
        catalogTitle.addClassName("parameters-section-title");

        Paragraph catalogDescription = new Paragraph(
                "Перегляд визначених параметрів, їхніх типів та одиниць вимірювання."
        );
        catalogDescription.addClassName("parameters-description");

        catalogCard.add(catalogTitle, catalogDescription, parametersGrid);

        Div createCard = new Div();
        createCard.addClassName("parameters-card");

        H2 createTitle = new H2("Створити параметр");
        createTitle.addClassName("parameters-section-title");

        Paragraph createDescription = new Paragraph(
                "Вкажіть код, назву, тип даних та інші характеристики параметра."
        );
        createDescription.addClassName("parameters-description");

        createCard.add(createTitle, createDescription, createParameterForm());

        feedbackContainer.addClassName("parameters-feedback");

        add(
                title,
                subtitle,
                feedbackContainer,
                catalogCard,
                createCard
        );

        refreshData();
    }

    private void configureParametersGrid() {
        parametersGrid.addClassName("parameters-grid");

        parametersGrid.addColumn(ParameterDefinitionDto::code)
                .setHeader("Код")
                .setAutoWidth(true);

        parametersGrid.addColumn(ParameterDefinitionDto::name)
                .setHeader("Назва")
                .setAutoWidth(true)
                .setFlexGrow(1);

        parametersGrid.addColumn(ParameterDefinitionDto::type)
                .setHeader("Тип")
                .setAutoWidth(true);

        parametersGrid.addColumn(parameter ->
                        parameter.unit() == null || parameter.unit().isBlank()
                                ? "—"
                                : parameter.unit()
                )
                .setHeader("Одиниця")
                .setAutoWidth(true);

        parametersGrid.addColumn(parameter ->
                        parameter.description() == null
                                || parameter.description().isBlank()
                                ? "—"
                                : parameter.description()
                )
                .setHeader("Опис")
                .setAutoWidth(true)
                .setFlexGrow(2);

        parametersGrid.setWidthFull();
        parametersGrid.setMinHeight("200px");
        parametersGrid.setMaxHeight("420px");
    }

    private void configureParameterForm() {
        codeField.setPlaceholder("temperature");
        codeField.setHelperText(
                "Унікальний код, наприклад temperature"
        );
        codeField.setRequired(true);
        codeField.setMaxLength(100);
        codeField.setClearButtonVisible(true);

        nameField.setPlaceholder("Температура");
        nameField.setRequired(true);
        nameField.setMaxLength(150);
        nameField.setClearButtonVisible(true);

        descriptionField.setPlaceholder(
                "Опис призначення параметра"
        );
        descriptionField.setMaxLength(500);
        descriptionField.setWidthFull();

        dataTypeField.setItems(ParameterDataType.values());
        dataTypeField.setItemLabelGenerator(Enum::name);
        dataTypeField.setRequired(true);
        dataTypeField.setPlaceholder("Виберіть тип");

        unitField.setPlaceholder("°C");
        unitField.setMaxLength(30);
        unitField.setClearButtonVisible(true);
    }

    private FormLayout createParameterForm() {
        FormLayout form = new FormLayout();
        form.addClassName("parameters-form");

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

        createButton.addClassName("parameters-primary-button");

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

        sensorDeviceSelect.setItemLabelGenerator(
                device -> device.getDeviceName()
                        + " (" + device.getDeviceId() + ")"
        );
        sensorDeviceSelect.setWidthFull();
    }

    private void createParameter() {
        String code = codeField.getValue();
        String name = nameField.getValue();
        String description = descriptionField.getValue();
        String unit = unitField.getValue();
        ParameterDataType dataType = dataTypeField.getValue();

        if (code == null || code.isBlank()
                || name == null || name.isBlank()
                || dataType == null) {

            showFeedback(
                    "Заповніть код, назву та тип параметра.",
                    true
            );
            return;
        }

        try {
            ParameterDefinitionRegistrationDto parameter =
                    new ParameterDefinitionRegistrationDto(
                            code.trim(),
                            name.trim(),
                            description == null ? null : description.trim(),
                            dataType,
                            unit == null || unit.isBlank() ? null : unit.trim()
                    );

            parameterDefinitionService.createParameterDefinition(parameter);

            clearParameterForm();
            refreshData();

            showFeedback("Параметр успішно створено.", false);

        } catch (RuntimeException ex) {
            showFeedback(
                    "Не вдалося створити параметр: "
                            + (ex.getMessage() == null
                            ? "невідома помилка"
                            : ex.getMessage()),
                    true
            );
        }
    }

    private void refreshData() {
        try {
            List<ParameterDefinitionDto> parameters =
                    parameterDefinitionService
                            .getAllParameterDefinitions()
                            .stream()
                            .map(parameterDefinitionMapper::toDto)
                            .toList();

            parametersGrid.setItems(parameters);
            parameterSelect.setItems(parameters);

            List<ua.home.heatingautomation2026.entity.Device> devices =
                    deviceService.findAllDevices();

            deviceSelect.setItems(devices);
            sensorDeviceSelect.setItems(devices);

        } catch (RuntimeException ex) {
            showFeedback(
                    "Не вдалося завантажити параметри або пристрої.",
                    true
            );
        }
    }

    private void clearParameterForm() {
        codeField.clear();
        nameField.clear();
        descriptionField.clear();
        dataTypeField.clear();
        unitField.clear();
    }

    private void showFeedback(String message, boolean error) {
        feedbackContainer.removeAll();

        Paragraph notification = new Paragraph(message);
        notification.addClassName(
                error
                        ? "parameters-feedback-error"
                        : "parameters-feedback-success"
        );

        feedbackContainer.add(notification);
    }
}
