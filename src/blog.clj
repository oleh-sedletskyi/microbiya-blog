(ns blog
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [hiccup2.core :as h]
   [markdown.core :as md]
   [files :as file])
  (:gen-class))

;; metadata in Markdown
;; https://quarto.org/docs/authoring/front-matter.html
(defn render-post-html [post]
  (let [template (slurp "template/post.html")
        updated (str/replace template #"CONTENT-TO-REPLACE" (:html post))
        html-file (-> (get-in post [:metadata :md-path])
                      (file/file-path->name "html"))
        output-file (str "public/posts/" html-file)
        relative-file-path (str "posts/" html-file)]
    ;; TODO: create only once
    (file/make-parents-dirs output-file)
    (spit output-file updated)
    relative-file-path))

(defn render-index-html [main-html]
  (let [template (slurp "template/index.html")
        updated (str/replace template #"MAIN-TO-REPLACE" main-html)
        output-file (str "public/index.html")]
    (println (str "Render " output-file))
    (file/make-parents-dirs output-file)
    (spit output-file updated)))

(defn parse-md-file [path]
  (let [md-content (slurp path)
        m (md/md-to-html-string-with-meta md-content)]
    (-> (assoc m :markdown md-content)
        (update-in [:metadata] merge {:md-path path}))))

(defn render-main-page
  [{:keys [metadata html]}]
  (let [template (slurp "template/index.html")
        html-content (-> (h/html
                          [:main {:class "container"}
                           [:section
                            (h/raw
                             html)]])
                         str)
        updated (str/replace template #"MAIN-TO-REPLACE" html-content)
        html-file (-> (get metadata :md-path)
                      (file/file-path->name "html"))
        output-file (str "public/" html-file)]
    (println (str " " output-file))
    ;; TODO: create only once
    (file/make-parents-dirs output-file)
    (spit output-file updated)))

(defn render-main-pages []
  (let [main-files (->> (file/list-dir-files "content")
                        (filter #(some (partial str/includes? %) ["kontakty" "pro-mikrob-i-ya"])))]
    (when (seq main-files)
      (println  "Render main files:"))
    (->> main-files
         (map parse-md-file)
         (mapv render-main-page))))

(defn process-posts
  [_]
  (let [posts-path "content/post/"
        files (file/list-dir-files posts-path)
        parsed-files (->> files
                          (map parse-md-file)
                          (sort-by (fn [m] (-> (get-in m [:metadata :created])
                                               ;; make sure date is as str
                                               str))
                                   #(compare %2 %1)))
        _ (println (str "Render " (count parsed-files) " posts"))
        rendered-files (->> parsed-files
                            (map render-post-html))
        metadata (->> (mapv (fn [m html-path]
                              (-> (assoc-in m [:metadata :html-path] html-path)
                                  :metadata
                                  (dissoc :markdown)))
                            parsed-files rendered-files))
        _images (->> (mapcat #(get-in % [:metadata :images]) parsed-files)
                     (mapv identity))]
    (-> (h/html
         [:main {:class "container"}
          (->> metadata
               (map (fn [{:keys [introtext created title html-path]}]
                      [:section
                       [:hgroup
                        [:h2
                         [:a {:href html-path :class "contrast"} title]]
                        [:small created]
                        (h/raw introtext)]])))

          [:hr]])
        str
        (render-index-html))
    (render-main-pages)))

