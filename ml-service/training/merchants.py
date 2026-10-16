"""
Real merchant names per category, used by generate_data.py.

The names were taken only from the public sources listed below, not from
the private real transactions, so the real data stays a fair test.
All sources accessed on 2026-10-15.

[S1] Wikipedia, "List of supermarket chains in Germany"
     https://en.wikipedia.org/wiki/List_of_supermarket_chains_in_Germany
[S2] ecommerceGermany, "Top 100 online stores in Germany"
     https://ecommercegermany.com/blog/top-100-online-stores-in-germany/
[S3] Wikipedia, "List of supermarket chains in Europe"
     https://en.wikipedia.org/wiki/List_of_supermarket_chains_in_Europe
[S4] Expatrio, "10 best travel apps in Germany"
     https://www.expatrio.com/about-germany/10-best-travel-apps-germany
[S5] Live in Germany, "Best e-scooters in Germany"
     https://liveingermany.de/best-escooters-in-germany/
[S6] Expatrio, "8 best food and grocery delivery apps in Germany"
     https://www.expatrio.com/about-germany/8-best-food-and-grocery-delivery-apps-germany
[S7] Deutschlandtest, "Top Schnellrestaurants"
     https://deutschlandtest.de/ratgeber/top-schnellrestaurants
[S8] RevenueBase, "Bakery cafe chains in Germany"
     https://revenuebase.ai/companies/bakery-cafe-chains/germany
[S9] FlixPatrol, "Streaming services by subscribers, Germany"
     https://flixpatrol.com/streaming-services/subscribers/germany/
[S10] Simple Germany, "Best mobile network in Germany"
     https://www.simplegermany.com/best-mobile-network-germany/
[S11] Similarweb, "Top accommodation and hotel websites in Germany"
     https://www.similarweb.com/top-websites/germany/travel-and-tourism/accommodation-and-hotels/
[S12] Expatrio, "10 best movie theaters and film festivals in Germany"
     https://www.expatrio.com/about-germany/10-best-movie-theaters-and-film-festivals-germany

Category mapping follows the app categories: drugstores and grocery delivery
count as Groceries, beauty shops as Shopping, online pharmacies as Health,
bakeries as Eating Out, streaming as Subscriptions, mobile providers as
Utilities. The app has no travel category, so accommodation is Uncategorized.
Legal suffixes (GmbH, KG, ...) were removed; the generator adds them.
"""

MERCHANTS = {
    "Groceries": [
        # [S1] discounters, supermarkets, hypermarkets, convenience stores
        "Aldi Süd", "Aldi Nord", "Lidl", "Netto Marken-Discount", "Norma", "Penny Markt", "Netto",
        "Alnatura", "Bio Company", "CAP Markt", "Hit", "nah & frisch", "nah & gut", "tegut", "nahkauf",
        "Combi", "Globus", "famila", "Kaufland", "Marktkauf", "real", "Metro", "Selgros",
        "Edeka", "REWE", "SPAR express",
        # [S3] other European chains
        "Carrefour", "Auchan", "SPAR", "Coop", "Migros", "Denner",
        "Marks & Spencer", "Waitrose", "Iceland", "Mega Image", "Profi",
        # [S2] grocery delivery and drugstores
        "dm", "Rossmann", "Müller", "Flaschenpost", "Picnic", "Bringmeister", "Edeka24", "MyTime",
        # [S6] grocery delivery and meal kits
        "Gorillas", "Flink", "Getir", "HelloFresh",
    ],
    "Shopping": [
        # [S2] general retail and marketplaces
        "Amazon", "eBay", "Otto", "Temu", "AliExpress", "Tchibo", "Galeria", "Baur", "QVC",
        # [S2] fashion and shoes
        "Zalando", "Shein", "About You", "Bonprix", "H&M", "C&A", "Breuninger", "Peek & Cloppenburg",
        "Deichmann", "Snipes", "Adidas", "Nike", "BestSecret", "Lascana", "Ernsting's family", "Zara",
        # [S2] beauty
        "Douglas", "Flaconi", "Notino", "Sephora",
        # [S2] electronics and appliances
        "MediaMarkt", "Saturn", "Samsung", "Cyberport", "Notebooksbilliger", "Alternate", "Conrad",
        "Expert", "Mindfactory", "Miele", "Bosch",
        # [S2] home, furniture, DIY, garden
        "IKEA", "Home24", "Wayfair", "Westwing", "XXXLutz", "Roller", "Höffner", "Porta",
        "OBI", "Hornbach", "Bauhaus", "Toom", "Hagebau", "Hellweg", "Globus Baumarkt", "Dehner",
        "ManoMano", "Butlers",
        # [S2] books, media, recommerce
        "Thalia", "Hugendubel", "Medimops", "Momox", "Rebuy", "Kleinanzeigen", "Vinted",
        "Booklooker", "JPC", "GameStop",
        # [S2] sports, outdoor, bikes, pets
        "Decathlon", "Intersport", "SportScheck", "Bergfreunde", "Bergzeit", "Bike24", "Rose Bikes",
        "Canyon", "Tennis-Point", "Zooplus", "Fressnapf", "Bitiba", "ZooRoyal", "Equiva",
    ],
    "Health": [
        # [S2] online pharmacies
        "Shop Apotheke", "DocMorris", "Medpex", "Medikamente per Klick", "Aponeo",
    ],
    "Transport": [
        # [S4] travel apps
        "Deutsche Bahn", "DB Navigator", "BahnCard", "FlixBus", "FlixTrain", "BlaBlaCar",
        "Free Now", "Taxi.eu", "Uber", "Bolt", "Lime", "Nextbike", "Tier",
        # [S5] e-scooter sharing
        "Bird", "Voi",
    ],
    "Eating Out": [
        # [S4], [S6] food delivery
        "Uber Eats", "Lieferando", "Wolt",
        # [S7] fast food and quick service restaurants
        "Hans im Glück", "Peter Pane", "Frittenwerk", "McDonald's", "Five Guys", "Vapiano",
        "Burgermeister", "Kentucky Fried Chicken", "KFC", "Burger King", "Subway", "Pizza Hut",
        "dean&david",
        # [S8] bakery and cafe chains
        "Junge Die Bäckerei", "Kamps", "Malzers Backstube", "Le Crobag", "Papperts", "Brothaus",
        "Sehne Backwaren", "Achim Lohner", "Höflinger Müller", "Lukas Bäcker", "Cookie Couture",
        "Bäckerei Hoefer", "Schwälmer Brotladen", "von Allwörden", "Bäckerei Bachmeier",
        "Stadtbäckerei", "Bäckerei Bolten", "Backhaus Kutzer", "Backhaus Lüning",
        "Landbäckerei Sommer", "Landbäckerei Gagel", "Bäckerei Gehr", "Stadtbäckerei Engelbrecht",
        "Bäckerei Evertzberg", "Stadtbäckerei Dreher",
    ],
    "Subscriptions": [
        # [S9] streaming services
        "Netflix", "Amazon Prime", "RTL+", "Disney+", "Discovery+", "Crunchyroll", "Apple TV+",
        "WOW", "Paramount+", "HBO Max", "iQIYI", "CuriosityStream", "DAZN", "Starz", "Viaplay", "Joyn",
    ],
    "Entertainment": [
        # [S12] movie theatres
        "CineStar", "UCI Kinowelt", "Babylon Mitte", "Das Kino", "Filmhaus", "CinemaxX",
    ],
    "Utilities": [
        # [S10] mobile network providers
        "Vodafone", "Telekom", "Deutsche Telekom", "T-Mobile", "1&1", "freenet", "O2", "Telefónica",
    ],
    "Uncategorized": [
        # [S11] accommodation and hotel websites
        "Booking.com", "Airbnb", "Check24", "FeWo-direkt", "Expedia",
    ],
}
