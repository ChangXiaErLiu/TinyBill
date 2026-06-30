package com.tinybill.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 分类图标名称到 ImageVector 的统一映射
 *
 * 之前分散在 [com.tinybill.ui.screen.AddTransactionDialog] 与
 * [com.tinybill.ui.screen.SettingsScreen] 两个文件中，其中
 * AddTransactionDialog 内的版本在 Composable 函数体内定义，**每次重组都会重建 600+ 项 map**。
 *
 * 统一提到顶层后由 Kotlin 类加载器单例化，所有 UI 共享同一份引用。
 */
val CATEGORY_ICON_MAP: Map<String, ImageVector> = mapOf(
    "restaurant" to Icons.Default.Restaurant,
    "shopping_bag" to Icons.Default.ShoppingBag,
    "home" to Icons.Default.Home,
    "bolt" to Icons.Default.Bolt,
    "directions_bus" to Icons.Default.DirectionsBus,
    "movie" to Icons.Default.Movie,
    "face" to Icons.Default.Face,
    "more_horiz" to Icons.Default.MoreHoriz,
    "attach_money" to Icons.Default.AttachMoney,
    "favorite" to Icons.Default.Favorite,
    "swap_horiz" to Icons.Default.SwapHoriz,
    "add_circle" to Icons.Default.AddCircle,
    "local_cafe" to Icons.Default.LocalCafe,
    "local_grocery_store" to Icons.Default.LocalGroceryStore,
    "local_hospital" to Icons.Default.LocalHospital,
    "school" to Icons.Default.School,
    "sports_esports" to Icons.Default.SportsEsports,
    "flight" to Icons.Default.Flight,
    "pets" to Icons.Default.Pets,
    "card_giftcard" to Icons.Default.CardGiftcard,
    "fitness_center" to Icons.Default.FitnessCenter,
    "local_gas_station" to Icons.Default.LocalGasStation,
    "local_parking" to Icons.Default.LocalParking,
    "train" to Icons.Default.Train,
    "subway" to Icons.Default.Subway,
    "local_taxi" to Icons.Default.LocalTaxi,
    "star" to Icons.Default.Star,
    "savings" to Icons.Default.Savings,
    "account_balance" to Icons.Default.AccountBalance,
    "credit_card" to Icons.Default.CreditCard,
    "build" to Icons.Default.Build,
    "child_care" to Icons.Default.ChildCare,
    "wc" to Icons.Default.Wc,
    "local_bar" to Icons.Default.LocalBar,
    "local_mall" to Icons.Default.LocalMall,
    "local_pharmacy" to Icons.Default.LocalPharmacy,
    "spa" to Icons.Default.Spa,
    "checkroom" to Icons.Default.Checkroom,
    "money" to Icons.Default.Money,
    "payment" to Icons.Default.Payment,
    "receipt" to Icons.Default.Receipt,
    "shopping_cart" to Icons.Default.ShoppingCart,
    "store" to Icons.Default.Store,
    "work" to Icons.Default.Work,
    "attach_file" to Icons.Default.AttachFile,
    "local_dining" to Icons.Default.LocalDining,
    "computer" to Icons.Default.Computer,
    "wallet" to Icons.Default.AccountBalanceWallet,
    "car" to Icons.Default.DirectionsCar,
    "bike" to Icons.Default.PedalBike,
    "bus" to Icons.Default.DirectionsBus,
    "walk" to Icons.Default.DirectionsWalk,
    "gift" to Icons.Default.CardGiftcard,
    "cake" to Icons.Default.Cake,
    "beach" to Icons.Default.BeachAccess,
    "smoking_rooms" to Icons.Default.SmokingRooms,
    "snowmobile" to Icons.Default.Snowmobile,
    "store_mall_directory" to Icons.Default.StoreMallDirectory,
    "streetview" to Icons.Default.Streetview,
    "takeout_dining" to Icons.Default.TakeoutDining,
    "taxi_alert" to Icons.Default.TaxiAlert,
    "terrain" to Icons.Default.Terrain,
    "traffic" to Icons.Default.Traffic,
    "tram" to Icons.Default.Tram,
    "transfer_within_a_station" to Icons.Default.TransferWithinAStation,
    "transit_enterexit" to Icons.Default.TransitEnterexit,
    "trip_origin" to Icons.Default.TripOrigin,
    "two_wheeler" to Icons.Default.TwoWheeler,
    "volunteer_activism" to Icons.Default.VolunteerActivism,
    "wine_bar" to Icons.Default.WineBar,
    "wrong_location" to Icons.Default.WrongLocation,
    "zoom_out_map" to Icons.Default.ZoomOutMap
)
