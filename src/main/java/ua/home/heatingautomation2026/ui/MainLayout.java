package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class MainLayout extends AppLayout {

    public MainLayout() {

        // Заголовок застосунку
        H2 appName = new H2("Home Heating");

        appName.getStyle()
                .set("font-size", "1.2rem")
                .set("margin", "0");

        // Верхня панель
        addToNavbar(
                new DrawerToggle(),
                appName
        );

        // Бокове меню
        SideNav navigation = new SideNav();

        navigation.addItem(
                new SideNavItem("Огляд", DashboardView.class)
        );

        navigation.addItem(
                new SideNavItem("Пристрої", DevicesView.class)
        );

        navigation.addItem(
                new SideNavItem("Датчики", SensorsView.class)
        );
        navigation.addItem(
                new SideNavItem("Параметри", ParametersView.class)
        );

        addToDrawer(navigation);

        // Бокове меню займає повну висоту
        setPrimarySection(Section.DRAWER);
    }
}