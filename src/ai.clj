(ns ai
  (:require [cheshire.core :as json]
            [config]
            [org.httpkit.client :as hk-client]))

(defn token
  []
  (-> (config/read "resources/secrets.edn" config/Config)
      :openai
      :token))

(def models ["gpt-5-nano"
             "gpt-5-mini"
             "gpt-5"
             "gpt-4.1-2025-04-14"])

(def prompt-keywords-n-description
  (str "You are an assistant that extracts SEO metadata from content. "
       "Given the following markdown document, return a JSON object with two fields: "
       "`keywords` (a list of 5–10 relevant keywords or phrases), and "
       "`description` (a concise 1–2 sentence summary). "
       "The JSON should be compact and valid in the language of the document."
       "Do not include any explanations.\n\n"
       "Markdown Content:\n\n"))

(defn ask [input]
  (->> @(hk-client/post "https://api.openai.com/v1/responses"
                        {:headers
                         {"Content-Type" "application/json"
                          "Authorization" (format "Bearer %s" token)}
                         :body
                         (json/encode
                          {:model (first models)
                           :input input
                           :reasoning
                           {:effort "minimal"}})})
       :body
       (#(json/decode % keyword))
       :output
       (filter #(some? (:content %)))
       first
       :content
       first
       :text
       (#(json/decode % keyword))))
