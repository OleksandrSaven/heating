package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import com.vaadin.flow.component.dependency.StyleSheet;

@StyleSheet("context://style.css")
public class MainLayout extends AppLayout {

    public MainLayout() {
        addClassName("main-layout");

        // Назва застосунку
        H2 appName = new H2("Heating Automation");

        appName.addClassName("main-layout-title");

        // Кнопка відкриття бокового меню
        DrawerToggle drawerToggle = new DrawerToggle();
        drawerToggle.addClassName("main-layout-drawer-toggle");

        // Верхня панель
        addToNavbar(drawerToggle, appName);

        // Бокове меню
        SideNav navigation = new SideNav();
        navigation.addClassName("main-layout-navigation");

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

        navigation.addItem(
                new SideNavItem("Котельня", BoilerRoomView.class)
        );

        addToDrawer(navigation);

        // Бокове меню — основна навігаційна секція
        setPrimarySection(Section.DRAWER);
    }
}
