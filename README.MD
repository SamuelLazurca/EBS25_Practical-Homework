# Subscriptions and Publications Generation

---

## Authors

- Lazurcă Samuel-Ionuț (MISS1)
- Nastasiu Ștefan (MISS1)
- Anfimov Vladimir (MIAO1)

## Specificatii procesor

````
=== System & CPU Stats ===
OS Name:         Windows 11
OS Version:      10.0
Architecture:    amd64
Available Cores: 16
````

## Explicatie algoritmi

---

### Generare publicatii

Pentru generarea de publicatii, algoritmul genereaza random fiecare field
fie dintr-un interval, fie dintr-o lista de valori predefinite.

Pentru varianta multi-threaded, algoritmul genereaza publicatiile in paralel, impartind
numarul de publicatii in mod aproximativ egal pe numarul de thread-uri.

```
PUBLICATIONS  (100000 publications)
===================================

Single-threaded execution

** Duration: 306 ms

Multi-threaded execution

=== Per-Thread Statistics ===
Thread   | Pubs       | Time (ms) 
----------------------------------
0        | 25000      |        216
1        | 25000      |        209
2        | 25000      |        216
3        | 25000      |        214

** Number of threads: 4
** Duration 223 ms
```

### Generare subscriptii

La acest pas, se transforma procentele pentru fiecare field in numere intregi.
Pe parcursul generarii se numara de cate ori apare un field.

Un caz important de tratat este atunci cand numarul total minus numarul curent de subscriptii devine
egal cu numarul ramas de aparitii a unui field. Astfel ca trebuie inclus obligatoriu acel field in toate subscriptiile
ramase de generat. Daca, insa, numarul de subscriptii ce mai trebuie generate pentru un camp este mai mic decat numarul total
de subscriptii ramase de construit, atunci se alege acest camp cu probabilitate de ```50%```.

Un alt caz special este atunci cand toate campurile au restrictii de frecventa, iar suma totala a acestor frecvente
este mai mica decat numarul total de subscriptii. Generarea tuturor subscriptiilor ar conduce la unele goale, astfel ca o exceptie
este aruncata. Daca este macar un field fara restrictie, generarea este posibila, deoarece fiecare subscriptie va avea cel putin
un camp.

La metoda multi-threaded, pentru a evita generarea de subscriptii goale (cum a fost descris mai sus), a fost implementata o
echilibrare a numarului de subscriptii si a numarului de campuri per thread. Astfel se distribuie "load-ul" in mod egal,
iar setul de subscriptii obinut respecta, totodata, restrictiile impuse de cerinta.

Se procedeaza intr-o maniera asemanatoare si pentru frecventa operatorului ```==```.

Frecventa minima pentru operatorul **EQ** poate fi setata pentru orice camp al subscriptiilor, restrictionat sau nu.

- ***Cu restrictie***: in acest caz, se poate calcula de la inceput numarul minim de operatori **EQ** pentru acel field;
- ***Fara restrictie***: in acest caz nu se poate afla de la inceput minimul de operatori **EQ** pentru un camp care are numar variabil
  de aparitii. Am rezolvat aceasta problema prin calcularea frecventei **EQ** minime la fiecare noua generare de subscriptie
  ce include campul vizat. Astfel, daca frecventa minima nu este atinsa, se foloseste direct operatorul **EQ** pentru campul ales
  din subscriptia generata. In cazul in care frecventa minima este atinsa, se genereaza un operator aleatoriu.

```
=== System & CPU Stats ===
OS Name:         Windows 11
OS Version:      10.0
Architecture:    amd64
Available Cores: 16


SUBSCRIPTIONS GENERATION (100000 subscriptions)
===============================================

Single-threaded execution

** Duration: 390 ms


Multi-threaded execution

=== Per-Thread Statistics ===
Thread   | Subs       | Time (ms)
----------------------------------
0        | 25000      |        290
1        | 25000      |        287
2        | 25000      |        280
3        | 25000      |        287

=== Fields Frequencies Report ===
Field           | Expected (Percent)   | Actual     | Diff
----------------------------------------------------------------
Date            | 50000 (50.00%)       | 50000      |         +0
Temp            | 50000 (50.00%)       | 50000      |         +0
Direction       | 50000 (50.00%)       | 50000      |         +0
Wind            | 30000 (30.00%)       | 30000      |         +0
Rain            | 50000 (50.00%)       | 50000      |         +0
Station         | 50000 (50.00%)       | 50000      |         +0
City            | 70000 (70.00%)       | 70000      |         +0

=== Equal Operators Frequencies Report ===
Field           | Expected   | Actual     | Diff
------------------------------------------------------
Date            | 25000      | 25000      |         +0
Temp            | 25000      | 25000      |         +0
Direction       | 25000      | 25000      |         +0
Wind            | 15000      | 15000      |         +0
Rain            | 25000      | 25000      |         +0
Station         | 25000      | 25000      |         +0
City            | 35000      | 35000      |         +0

** Number of threads: 4
** Duration 308 ms
```
