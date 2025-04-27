/**
 * 
 */
/**
 * 
 */
module assignement2_2024 {
	requires java.logging;
	requires javafx.graphics;
	requires javafx.controls;
	// allow the launcher to construct your Application subclass
	opens gui to javafx.graphics;
	// allow TableView (which lives in javafx.base/javafx.graphics) to reflectively read your model
    opens model to javafx.base, javafx.graphics;
}