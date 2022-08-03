(ns eyebrowse.main
  (:import [java.io File]
           [javax.imageio ImageIO]
           [eyebrowse JavaFXHtmlImageCapture]))

(defonce fx-thread (.initialize (JavaFXHtmlImageCapture.)))

(defn- new-thread?
  [thread]
  (= "NEW" (str (:state (bean thread)))))

(defn html->image
  [html-str width height]
  (when (new-thread? fx-thread)
    (.start fx-thread)
    (Thread/sleep 200))
  (JavaFXHtmlImageCapture/captureHtml html-str width height))

(defn save-image!
  [buffered-image filename]
  (ImageIO/write buffered-image "png" (File. filename)))
