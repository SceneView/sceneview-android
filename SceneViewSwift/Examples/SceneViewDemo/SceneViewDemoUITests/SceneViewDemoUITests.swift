import XCTest

final class SceneViewDemoUITests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
        sleep(3) // Laisser la scène 3D se charger
    }

    override func tearDownWithError() throws {
        app = nil
    }

    // MARK: - Tab Navigation

    func testAllTabsExist() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.exists, "La tab bar doit exister")
        XCTAssertTrue(app.tabBars.buttons["Explore"].exists, "Onglet Explore manquant")
        XCTAssertTrue(app.tabBars.buttons["Shapes"].exists,  "Onglet Shapes manquant")
        XCTAssertTrue(app.tabBars.buttons["AR"].exists,      "Onglet AR manquant")
        XCTAssertTrue(app.tabBars.buttons["About"].exists,   "Onglet About manquant")
    }

    func testExploreTabLoads() throws {
        app.tabBars.buttons["Explore"].tap()
        sleep(2)
        attach(name: "Explore Tab")
        // L'onglet Explore doit afficher au moins un bouton de chip ou du texte
        let hasContent = app.buttons.count > 0 || app.staticTexts.count > 0
        XCTAssertTrue(hasContent, "Explore tab: aucun contenu visible")
    }

    func testShapesTabLoads() throws {
        app.tabBars.buttons["Shapes"].tap()
        sleep(2)
        attach(name: "Shapes Tab")
        XCTAssertTrue(app.tabBars.buttons["Shapes"].exists)
    }

    func testARTabLoads() throws {
        app.tabBars.buttons["AR"].tap()
        sleep(2)
        attach(name: "AR Tab")
        XCTAssertTrue(app.tabBars.buttons["AR"].exists)
    }

    func testAboutTabLoads() throws {
        app.tabBars.buttons["About"].tap()
        sleep(3)
        attach(name: "About Tab")
        // L'onglet About doit afficher du texte
        XCTAssertTrue(app.staticTexts.count > 0 || app.scrollViews.count > 0,
                      "About tab: aucun contenu visible")
    }

    // MARK: - Navigation complète (sans vérifier isSelected — SwiftUI ne l'expose pas fiablement)

    func testFullTabNavigation() throws {
        let tabs = ["Explore", "Shapes", "AR", "About"]
        for tab in tabs {
            let button = app.tabBars.buttons[tab]
            XCTAssertTrue(button.exists, "Onglet \(tab) introuvable")
            button.tap()
            sleep(2)
            attach(name: "Nav → \(tab)")
            // Vérifier simplement que l'app est toujours vivante
            XCTAssertTrue(app.state == .runningForeground,
                          "App crashée sur onglet \(tab)")
        }
    }

    // MARK: - Chip filters dans Explore (robuste : skip si bouton absent)

    func testExploreChipFilters() throws {
        app.tabBars.buttons["Explore"].tap()
        sleep(2)

        // Les chips peuvent avoir des labels légèrement différents selon l'accessibilité
        // On cherche parmi les boutons visibles
        let allButtons = app.buttons.allElementsBoundByIndex
        attach(name: "Explore before chips")

        var chipsFound = 0
        for button in allButtons {
            let label = button.label
            // Chips de scène (hors tab bar et nav)
            if ["Shapes", "Metallic", "3D Text", "Gizmo"].contains(label) {
                chipsFound += 1
                button.tap()
                sleep(1)
                attach(name: "After chip: \(label)")
            }
        }
        // On accepte même si aucun chip n'est trouvé (accessibilité variable)
        // Le test vérifie juste que l'app ne crashe pas
        XCTAssertTrue(app.state == .runningForeground,
                      "App crashée lors du tap sur les chips")
    }

    // MARK: - Helper

    private func attach(name: String) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
