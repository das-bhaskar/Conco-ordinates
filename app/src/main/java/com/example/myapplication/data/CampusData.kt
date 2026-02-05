package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng

data class Building(
    val name: String,
    val code: String, // Added code for better identification
    val outline: List<LatLng>,
    val isCampusBuilding: Boolean = true
)

data class Campus(
    val name: String,
    val center: LatLng,
    val buildings: List<Building>,
    val defaultZoom: Float = 17f
)

object CampusRepo {
    private val sgwBuildings = listOf(
        // Henry F. Hall Building (H)
        Building(
            "Henry F. Hall Building", "H", listOf(
                LatLng(45.49685236883805, -73.57883714093387),
                LatLng(45.49719808458706, -73.57954979178562),
                LatLng(45.49779539207568, -73.57901882731855),
                LatLng(45.497392914985085, -73.57830213032375),

            )
        ),
        // J.W. McConnell Building / Library (LB)
        Building(
            "J.W. McConnell Building", "LB", listOf(
                LatLng(45.496679701508604, -73.57856854434014),
                LatLng(45.497294435061214, -73.57806066702534),
                LatLng(45.49692515475134, -73.57729799036079),

                LatLng(45.49661224025382, -73.57758071998815),//
                LatLng(45.49650793503491, -73.57745869983319),//
                LatLng(45.496263146898855, -73.57770724724443)
            )
        ),
        // John Molson Building (MB)
        Building(
            "John Molson Building", "MB", listOf(
                LatLng(45.494960246925125, -73.57881201129592),
                LatLng(45.49536580070366, -73.57948743576613),
                LatLng(45.495639226715895, -73.57925182257885),
                LatLng(45.495530957514134, -73.57890887449516),
                LatLng(45.49520981882679, -73.57852142169828),

            )
        ),

                // Engineering, Computer Science and Visual Arts Integrated Complex (EV)
                Building(
                "Engineering, Computer Science and Visual Arts Integrated Complex",
        "EV",
        listOf(
            LatLng(45.495344645848554, -73.57798777765004),
            LatLng(45.49552886671219, -73.57776758551564),
            LatLng(45.49595456397014, -73.57841750746071),
            LatLng(45.49562720057515, -73.57872115951703),
            LatLng(45.495440490562295, -73.57837488962826),


                    LatLng(45.49540314848542, -73.57825591484595),
// Removed the comma here
        )
    ),

       // Faubourg Building (FB)

        Building(
            "Faubourg Building",
            "FB",
            listOf(
                LatLng(45.49471650120424, -73.5780251591267),
                LatLng(45.49497736731041, -73.57777156217453),
                LatLng(45.49467242121655, -73.57720251535505),
                LatLng(45.49440143692754, -73.57751177993086),


            )
        ),


        //Faubourg Ste-Catherine Building (FG)

        Building(
            "Faubourg Ste-Catherine Building",
            "FG",
            listOf(
                LatLng(45.49382544345299, -73.57903916506176),
                LatLng(45.49363930421001, -73.5787307110219),
                LatLng(45.4945060685784, -73.57768464949552),
                LatLng(45.494709126414925, -73.57803065446195),


                )
        ),



        //Guy-De Maisonneuve Building (GM)

        Building(
            "Guy-De Maisonneuve Building",
            "GM",
            listOf(
                LatLng(45.4956396329284, -73.57876471031153),
                LatLng(45.49580908948487, -73.57912124572488),
                LatLng(45.49620164156328, -73.57878731987432),
                LatLng(45.49598707927566, -73.57841687088388),


                )
        ),

    )



    val SGW = Campus("SGW", LatLng(45.4973, -73.5790), sgwBuildings)
    val LOYOLA = Campus("Loyola", LatLng(45.4582, -73.6405), emptyList())
}