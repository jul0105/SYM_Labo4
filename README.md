# SYM : Labo 3 - Environnement I (Codes-barres, iBeacons et NFC)

> Auteurs : Julien Béguin, Robin Cuénoud & Gaëtan Daubresse
> Date : 07.01.2021
> Classe : B

## 1. Introduction

Ce laboratoire est la seconde partie du travail concernant l’utilisation de données environnementales, celui-ci est consacré aux capteurs disponibles sur les smartphones (accéléromètre et magnétomètre principalement) ainsi qu’à la communication Bluetooth Low Energy.

## 2. Capteurs

> Une fois la manipulation effectuée, vous constaterez que les animations de la flèche ne sont pas fluides, il va y avoir un tremblement plus ou moins important même si le téléphone ne bouge pas. Veuillez expliquer quelle est la cause la plus probable de ce tremblement et donner une manière (sans forcément l’implémenter) d’y remédier.

Nous avons effectivement constaté un tremblement sur le flèche. 

Cela est probablement dû à la précision des capteurs. En effet, les valeurs retournés par les capteurs comporte du "bruit" car même si l'appareil ne bouge pas, les valeurs retournées sont légèrement différentes. Ces différences peuvent être plus ou moins grande. A cet effet, chaque capteur indique son niveau de précision avec les valeurs suivantes :

```
SENSOR_STATUS_ACCURACY_HIGH : 3
SENSOR_STATUS_ACCURACY_MEDIUM : 2
SENSOR_STATUS_ACCURACY_LOW : 1
SENSOR_STATUS_UNRELIABLE : 0
SENSOR_STATUS_NO_CONTACT : -1
```

Ainsi, plus la précision est basse, plus le bruit peut être élevé.

Pour avoir un rendu plus précis, une solution serait d'accepter uniquement les données des capteurs indiquant une haute précision. Cela peut être implémenté avec la méthode `onAccuracyChanged` qui est appelé dès que la précision d'un capteur que l'on écoute est changé. Il nous est alors possible de stocker la précision de nos capteurs ce qui nous permettra de filtrer uniquement les valeurs des capteurs indiquant un précision haute. Le problème avec cette solution est que si, pendant un periode de temps, aucun capteur n'indique un précision haute, la boussole ne sera pas du tout mise à jour. Même en cas de mouvement de l'appareil.

Une autre solution serait de "lisser" le bruit en appliquant une moyenne des valeurs sur le temps. Ainsi, une valeur qui change brusquement sans raison serait adoucie à l'affichage. 



Sources :

- onAccuracyChanged : https://developer.android.com/reference/android/hardware/SensorEventListener#onAccuracyChanged(android.hardware.Sensor,%20int)
- SENSOR_STATUS_ACCURACY : https://developer.android.com/reference/android/hardware/SensorManager#SENSOR_STATUS_ACCURACY_HIGH



## 3. Communication *Bluetooth Low Energy*

TODO

## 4. Conclusion

TODO