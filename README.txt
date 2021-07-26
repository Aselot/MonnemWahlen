Hallo Herr Plugge,
Hallo Frau Fimmel,

Hallo an andere die an diesem Systen interessiert sind.

Dies ist eine Kurzanleitung für die Einstellung und Bedienung von dem System.

Um das System startfähig zu machen müssen folgende Sachen gemacht werden:

D0.1) Installieren von allen nötigen Technologien: Java MariaDB

D1) Erstellung der datenbank user für jede Entität welche aus den modulnammen abkürzung als user name
und modulname + 'pass'als passwort  :
    authenticator : username: "auth", passwort : "authpass"
    EW : username: "ew", passwort : "ewpass"
    vallier : username: "val", passwort : "valpass"
    tallier : username: "tal", passwort : "talpass"

+ einen creator:

    creator : username : "creator", passwort : "creatorpass"

die benötigten credentials kann man auch in den jeweiligen Service Klassen nachschauen, wo sie
 als variablen definiert wurden (siehe ElectionService, Zeile 40-41)
Alle User sollten zumindest INSERT, CREATE rechte haben.

D2) Ausführen von DatabaseCreator in monnemwahlen/mw/DatabaseCreator.java. Dies erzeugt alle benötigten
Listen und Datenbanken.

D3) Einfügung von wahlberechtigten Usern in authenticatordb.voters. Hier fügen Sie bitte mindesten einen User ein.
    Die query könnte so aussehen: "INSERT INTO voters (NAME,email,password) VALUE ("Albrecht","","23456");"



Ab diesem Punkt ist das system startbereit.

Das Starten des System erfolgt durch das Starten der Entitäten in einer gewissen Reihenfolge. Dabei sollte man nach
jedem Start einer Entität knapp 5 - 10 Sekunden warten bevor man die nächste startet:

1) Starten des Authenticator in mw/auth/src/main/java/auth/AuthApplication.java
2) Starten des Validator in mw/validator/src/main/java/auth/ValidatorApplication.java
3) Starten des Authenticator in mw/tallier/src/main/java/auth/TallierApplication.java
4) Starten des ElectionWorkers in mw/EW/src/main/java/auth/ElectionWorkerApplication.java



Dannach kann man den login page unter http://localhost:8081 aufrufen, und sich mit dem username und passwort einloggen,
welche man mit Schritt D3) angelegt hat

Nach dem einloggen wird man auf eine Wahlschein html page geleitet, da kann man nach belieben seine voting points verteilen.

Nachdem man diese getahn hat und eine Tracking Number bekommen hat, kann man auf der seite http://localhost:8084/showTally
den jetzigen Stand der Tally einsehen.


Wenn Sie das System fürs Internet mit Port forwarding öffnen möchten, können Sie bei dem Interface
mw/tallier/src/main/java/monnemwahlen/mw/MoService.java die IP addresse nach Ihrem umbennen.