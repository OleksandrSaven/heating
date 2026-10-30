package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ua.home.heatingautomation2026.enam.DeviceConnectivityStatus;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.service.DeviceConnectivityService;
import ua.home.heatingautomation2026.service.DeviceService;

import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Огляд | Home Assistant")
public class DashboardView extends VerticalLayout {

    private final DeviceService deviceService;
    private final DeviceConnectivityService deviceConnectivityService;

    private final Div devicesContainer = new Div();

    private final Button refreshButton = new Button(
            "Оновити",
            VaadinIcon.REFRESH.create()
    );

    public DashboardView(
            DeviceService deviceService,
            DeviceConnectivityService deviceConnectivityService
    ) {
        this.deviceService = deviceService;
        this.deviceConnectivityService = deviceConnectivityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        addClassName("dashboard-view");

        H1 title = new H1("Система");
        title.addClassName("dashboard-title");

        //refreshButton.addClassName("dashboard-refresh");
        //refreshButton.addClickListener(event -> refreshData());

        HorizontalLayout header = new HorizontalLayout(
                title
                //refreshButton
        );

        header.addClassName("dashboard-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);

        H2 devicesTitle = new H2("Зареєстровані пристрої");
        devicesTitle.addClassName("devices-section-title");

        devicesContainer.addClassName("devices-list");

        add(header, devicesTitle, devicesContainer);

        refreshData();
    }

    private Div createDeviceCard(Device device) {
        Div card = new Div();
        card.addClassName("device-card");

        Span statusIndicator = new Span();
        statusIndicator.addClassName("device-status-indicator");

        DeviceConnectivityStatus status =
                deviceConnectivityService.getDeviceConnectivityStatus(
                        device.getDeviceId()
                );

        switch (status) {
            case ONLINE -> {
                statusIndicator.addClassName("device-status-online");
                statusIndicator.getElement().setAttribute("title", "Пристрій онлайн");
                statusIndicator.getElement().setAttribute("aria-label", "Онлайн");
            }
            case OFFLINE -> {
                statusIndicator.addClassName("device-status-offline");
                statusIndicator.getElement().setAttribute("title", "Пристрій офлайн");
                statusIndicator.getElement().setAttribute("aria-label", "Офлайн");
            }
            case NEVER_CONNECTED -> {
                statusIndicator.addClassName("device-status-never-connected");
                statusIndicator.getElement().setAttribute(
                        "title",
                        "Пристрій ще не підключався"
                );
                statusIndicator.getElement().setAttribute(
                        "aria-label",
                        "Ще не підключався"
                );
            }
        }

        Span name = new Span(device.getDeviceName());
        name.addClassName("device-name");

        Span deviceId = new Span("ID: " + device.getDeviceId());
        deviceId.addClassName("device-id");

        Div deviceInfo = new Div(name, deviceId);
        deviceInfo.addClassName("device-info");

        Span arrow = new Span("›");
        arrow.addClassName("device-card-arrow");

        card.add(statusIndicator, deviceInfo, arrow);
        card.getElement().setAttribute("role", "link");
        card.getElement().setAttribute("tabindex", "0");
        card.getElement().setAttribute(
                "aria-label",
                "Відкрити пристрій " + device.getDeviceName()
        );

        card.addClassName("device-card-clickable");

        card.addClickListener(event ->
                UI.getCurrent().navigate(
                        "device/" + device.getDeviceId()
                )
        );

        card.addClickListener(event ->
                UI.getCurrent().navigate(
                        "device/" + device.getDeviceId()
                )
        );

        return card;
    }

    private void refreshData() {
        refreshButton.setEnabled(false);
        refreshButton.setText("Завантаження...");

        try {
            List<Device> devices = deviceService.findAllDevices();

            devicesContainer.removeAll();

            if (devices.isEmpty()) {
                Div emptyState = new Div();

                Span icon = new Span("⌁");
                icon.addClassName("devices-empty-icon");

                H2 emptyTitle = new H2("Пристроїв поки немає");
                emptyTitle.addClassName("devices-empty-title");

                Paragraph emptyMessage = new Paragraph(
                        "Зареєстровані пристрої з'являться тут після додавання до системи."
                );
                emptyMessage.addClassName("devices-empty-description");

                emptyState.add(icon, emptyTitle, emptyMessage);
                emptyState.addClassName("devices-empty");

                devicesContainer.add(emptyState);
            } else {
                devices.stream()
                        .map(this::createDeviceCard)
                        .forEach(devicesContainer::add);
            }

        } catch (RuntimeException exception) {
            devicesContainer.removeAll();

            Paragraph errorMessage = new Paragraph(
                    "Не вдалося завантажити пристрої. Перевірте з'єднання із сервером."
            );

            errorMessage.addClassName("devices-error");
            devicesContainer.add(errorMessage);

        } finally {
            refreshButton.setEnabled(true);
            refreshButton.setText("Оновити");
        }
    }
}