 package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import ua.home.heatingautomation2026.enam.DeviceConnectivityStatus;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.service.DeviceConnectivityService;
import ua.home.heatingautomation2026.service.DeviceService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Route(value = "devices", layout = MainLayout.class)
@PageTitle("Пристрої | Home Heating")
public class DevicesView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private final DeviceService deviceService;
    private final DeviceConnectivityService connectivityService;

    private Registration pollRegistration;

    private final Grid<Device> grid = new Grid<>(Device.class, false);

    public DevicesView(
            DeviceService deviceService,
            DeviceConnectivityService connectivityService
    ) {
        this.deviceService = deviceService;
        this.connectivityService = connectivityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("devices-view");

        H1 title = new H1("Пристрої");
        title.addClassName("devices-title");

        Button refreshButton = new Button(
                "Оновити",
                event -> refreshData()
        );
        refreshButton.addClassName("devices-refresh-button");

        HorizontalLayout header = new HorizontalLayout(title, refreshButton);
        header.addClassName("devices-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);

        Paragraph description = new Paragraph(
                "Реєстрація, статуси та керування ESP32-контролерами."
        );
        description.addClassName("devices-subtitle");

        configureGrid();

        Div card = new Div();
        card.addClassName("devices-card");

        H1 cardTitle = new H1("Зареєстровані пристрої");
        cardTitle.addClassName("devices-card-title");

        Paragraph cardDescription = new Paragraph(
                "Перегляд стану контролерів, часу останнього зв'язку "
                        + "та доступних дій."
        );
        cardDescription.addClassName("devices-card-description");

        card.add(cardTitle, cardDescription, grid);

        add(header, description, card);

        refreshData();
    }

    private void configureGrid() {
        grid.addClassName("devices-grid");

        grid.addColumn(Device::getDeviceId)
                .setHeader("ID пристрою")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(Device::getDeviceName)
                .setHeader("Назва")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(device ->
                        device.getDeviceType() == null
                                ? "—"
                                : device.getDeviceType().name()
                )
                .setHeader("Тип")
                .setAutoWidth(true);

        grid.addColumn(this::getStatusLabel)
                .setHeader("Статус")
                .setAutoWidth(true);

        grid.addColumn(this::getConnectivityLabel)
                .setHeader("Зв'язок")
                .setAutoWidth(true);

        grid.addColumn(device ->
                        formatTimestamp(device.getLastHeartbeat())
                )
                .setHeader("Останній зв'язок")
                .setAutoWidth(true);

        grid.addComponentColumn(this::createActions)
                .setHeader("Дії")
                .setAutoWidth(true);

        grid.setWidthFull();
        grid.setHeight("600px");
    }

    private HorizontalLayout createActions(Device device) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.addClassName("devices-row-actions");

        String status = device.getDeviceRegistrationStatus().name();

        switch (status) {
            case "PENDING" -> actions.add(
                    createAction(
                            "Активувати",
                            "devices-action-success",
                            "Пристрій активовано",
                            () -> deviceService.approveDevice(device.getId())
                    )
            );

            case "ACTIVE" -> actions.add(
                    createAction(
                            "Вимкнути",
                            "devices-action-warning",
                            "Пристрій вимкнено",
                            () -> deviceService.disableDevice(device.getId())
                    )
            );

            case "DISABLED" -> actions.add(
                    createAction(
                            "Увімкнути",
                            "devices-action-success",
                            "Пристрій знову активовано",
                            () -> deviceService.enableDevice(device.getId())
                    )
            );

            default -> {
                Button unavailableButton = new Button(
                        "Недоступно",
                        event -> Notification.show("Невідомий статус пристрою")
                );
                unavailableButton.addClassName("devices-action-button");
                unavailableButton.setEnabled(false);
                actions.add(unavailableButton);
            }
        }

        Button detailsButton = new Button("Деталі");
        detailsButton.addClassName("devices-details-button");
        detailsButton.addClickListener(event ->
                detailsButton.getUI().ifPresent(ui ->
                        ui.navigate("device/" + device.getDeviceId())
                )
        );

        actions.add(detailsButton);
        actions.add(createDeleteButton(device));

        return actions;
    }

    private Button createAction(
            String label,
            String styleClass,
            String successMessage,
            Runnable action
    ) {
        Button button = new Button(label);
        button.addClassName("devices-action-button");
        button.addClassName(styleClass);

        button.addClickListener(event -> {
            try {
                action.run();
                refreshData();

                Notification.show(
                        successMessage,
                        3000,
                        Notification.Position.BOTTOM_START
                );
            } catch (RuntimeException ex) {
                Notification.show(
                        "Помилка: " + ex.getMessage(),
                        5000,
                        Notification.Position.BOTTOM_START
                );
            }
        });

        return button;
    }

    private String getStatusLabel(Device device) {
        String status = device.getDeviceRegistrationStatus().name();

        return switch (status) {
            case "PENDING" -> "Очікує підтвердження";
            case "ACTIVE" -> "Активний";
            case "DISABLED" -> "Вимкнений";
            default -> status;
        };
    }

    private String getConnectivityLabel(Device device) {
        DeviceConnectivityStatus status =
                connectivityService.getDeviceConnectivityStatus(
                        device.getDeviceId()
                );

        return switch (status) {
            case ONLINE -> "🟢 Онлайн";
            case OFFLINE -> "🔴 Офлайн";
            case NEVER_CONNECTED -> "⚪ Не підключався";
        };
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();
        ui.setPollInterval(15_000);

        pollRegistration = ui.addPollListener(event -> refreshData());
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

    private String formatTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return "Ще не підключався";
        }

        return DATE_FORMAT.format(timestamp);
    }

    private void refreshData() {
        try {
            grid.setItems(deviceService.findAllDevices());
        } catch (RuntimeException ex) {
            Notification.show(
                    "Не вдалося завантажити пристрої: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_START
            );
        }
    }

    private Button createDeleteButton(Device device) {
        Button deleteButton = new Button("Видалити");
        deleteButton.addClassName("devices-delete-button");
        deleteButton.addThemeNames("error");

        deleteButton.addClickListener(event -> {
            ConfirmDialog dialog = new ConfirmDialog();

            dialog.setHeader("Видалити пристрій?");
            dialog.setText(
                    "Пристрій \"" + device.getDeviceName()
                            + "\" (" + device.getDeviceId() + "), "
                            + "усі призначені йому параметри та вся історія "
                            + "телеметрії будуть видалені назавжди. "
                            + "Визначення параметрів у загальному каталозі "
                            + "залишаться. Цю дію неможливо скасувати."
            );

            dialog.setCancelable(true);
            dialog.setCancelText("Скасувати");

            dialog.setConfirmText("Видалити назавжди");
            dialog.setConfirmButtonTheme("error primary");

            dialog.addConfirmListener(confirmEvent -> {
                try {
                    deviceService.deleteDevice(device.getId());
                    refreshData();

                    Notification.show(
                            "Пристрій та його історію видалено",
                            4000,
                            Notification.Position.BOTTOM_START
                    );
                } catch (RuntimeException ex) {
                    Notification.show(
                            "Не вдалося видалити пристрій: " + ex.getMessage(),
                            5000,
                            Notification.Position.BOTTOM_START
                    );
                }
            });

            dialog.open();
        });

        return deleteButton;
    }
}
