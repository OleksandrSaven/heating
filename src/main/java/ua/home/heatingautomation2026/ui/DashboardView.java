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

    public DashboardView(DeviceService deviceService, DeviceConnectivityService deviceConnectivityService) {
        this.deviceService = deviceService;
        this.deviceConnectivityService = deviceConnectivityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        addClassName("dashboard-view");

        H1 title = new H1("Система");
        title.addClassName("dashboard-title");

        refreshButton.addClickListener(event -> refreshData());
        refreshButton.addClassName("dashboard-refresh");

        HorizontalLayout header = new HorizontalLayout(title, refreshButton);
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

        Span name = new Span(
                getConnectivityLabel(device) + " " + device.getDeviceName()
        );
        name.addClassName("device-name");

        card.add(name);

        card.getStyle().set("cursor", "pointer");

        card.addClickListener(event ->
                UI.getCurrent().navigate(
                        "device/" + device.getDeviceId()
                )
        );

        return card;
    }

    private String getConnectivityLabel(Device device) {

        DeviceConnectivityStatus status =
                deviceConnectivityService.getDeviceConnectivityStatus(
                        device.getDeviceId()
                );

        return switch (status) {
            case ONLINE -> "🟢";
            case OFFLINE -> "🔴 ";
            case NEVER_CONNECTED -> "⚪";
        };
    }

    private void refreshData() {
        refreshButton.setEnabled(false);

        try {
            List<Device> devices = deviceService.findAllDevices();

            devicesContainer.removeAll();

            if (devices.isEmpty()) {
                Paragraph emptyMessage = new Paragraph(
                        "Зареєстрованих пристроїв поки немає."
                );
                emptyMessage.addClassName("devices-empty");
                devicesContainer.add(emptyMessage);
            } else {
                devices.stream()
                        .map(this::createDeviceCard)
                        .forEach(devicesContainer::add);
            }
        } catch (RuntimeException exception) {
            devicesContainer.removeAll();

            Paragraph errorMessage = new Paragraph(
                    "Не вдалося завантажити пристрої. Перевір з'єднання із сервером."
            );
            errorMessage.addClassName("devices-error");
            devicesContainer.add(errorMessage);
        } finally {
            refreshButton.setEnabled(true);
        }
    }
}