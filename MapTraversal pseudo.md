- func traverseMap():
	- switch case szerkezet a limitType-okhoz.
		 - case transferLimit: call transferLimit()
		 - case stopLimit: call stopLimit()  
		 - case timeLimit: call timeLimit()
	- visszatér a megoldás halmaz station-ökkel
	
- func trasferLimit(limit: int):
	- Kapcsolódó vonalak lekérdezése a kiinduló (src) állomástól
	- BFS alapú keresés: Meglátogatunk minden vonalat
	- Végig iterálunk a kapcsolódó vonalakon:
		- Adott vonalhoz tartozó állomások lekérése
		- BFS: Meglátogatjuk az összes elérhető állomást a vonalon
		- Végig iterálunk a vonalhoz tartozó állomásokon
			- Ha az állomáshoz kapcsolódik más vonal is, akkor:
				- call transferLimit(limit - 1)

- func stopLimit(limit: int):
	- BFS alapú keresés: Meglátogatjuk a jelenlegi állomást(src)
	- Ha a limit nagyobb mint 0, akkor:
		- Végig iterálunk a jelenlegi állomáshoz csatlakozó vonalakon:
			- Megkeressük a szomszédos megállókat, ami +1, -1 indexre vannak a jelenlegi(src) megállótól a vonalon
			- Szomszédos megállóból meghívjuk ugyanezt a stopLimit(limit -1) függvényt

- func timeLimit(limit: int):
	- Minden csatlakozó vonal lekérése a jelenlegi állomástól(src)
	- Végig iterálunk a kapcsolódó vonalakon:
		- Meglátogatjuk a jelenlegi állomást
		- Rendelkezésre álló idő csökkentése 1p-el
		- Állomások lekérdezése az adott vonalon
		- Szomszédos állomások megkeresése az adott vonalon, ami +1, -1 indexre vannak
		- Szomszédos állomásokra kiszámolni a távolságot, és ebből a szükséges időt (double érték)
		- Ha van elég idő rá, akkor
			- Szomszédos megállókból is indítjuk a bejárást timeLimit(limit-1-szükséges idő)
		- Kapcsolódó vonalak lekérése, és azon végig iterálás:
			- Ha van idő átszállni, és nem egyezik meg az adott vonallal az átszállási vonal, akkor:
				- bejárás timeLimit(limit-1-átszállási idő)



