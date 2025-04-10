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

Transform procentele pentru fiecare field in numare intregi. 
Pe parcursul generarii numar de cate ori apare un field.

Un caz important de tratat este atunci cand numarul total minus numarul curent de subscriptii devine 
egal cu numarul ramas de aparitii a unui field, atunci trebuie obligatoriu sa selectam acel field, 
altfel pot alege random.

Un alt caz special este atunci cand toate field-urile au restrictii de frecventa si 
suma lor e mai mica decat numarul total de subscriptii, atunci nu putem genera, daca avem insa field-uri fara restrictii 
putem alege dintre ele.

La metoda multi-threaded, trebuie sa ma asigur ca numarul total de field-uri este cel putin egal cu numarul 
de subscriptii de generat pentru a nu genera subscriptii goale asa ca trebuie facuta o echilibrare a numarului de subscriptii
si a numarului de field-uri per thread. 

Se procedeaza intr-o maniera asemanatoare si pentru frecventa operatorului "==".

Daca se seteaza o frecventa pentru operatorul egal dar nu pentru acel field, 
nu putem cunoaste inainte de rulare exact cati operatori ar trebui generati, dar am incercat sa rezolvam aceasta situatie 
prin calcularea numarului de aparitii ale operatorului egal in functie de numarul de aparitii a acelui field pana la
iteratia curenta.

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