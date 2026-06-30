package com.tinybill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tinybill.data.entity.Transaction
import com.tinybill.ui.theme.PrimaryGreen

object CategoryIconProvider {
    // 使用名称到图标的映射，确保保存和读取时一致
    val iconMap = mapOf(
        "restaurant" to Icons.Default.Restaurant,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "home" to Icons.Default.Home,
        "bolt" to Icons.Default.Bolt,
        "directions_bus" to Icons.Default.DirectionsBus,
        "movie" to Icons.Default.Movie,
        "face" to Icons.Default.Face,
        "fitness_center" to Icons.Default.FitnessCenter,
        "local_hospital" to Icons.Default.LocalHospital,
        "school" to Icons.Default.School,
        "flight" to Icons.Default.Flight,
        "pets" to Icons.Default.Pets,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "sports_esports" to Icons.Default.SportsEsports,
        "music_note" to Icons.Default.MusicNote,
        "book" to Icons.Default.Book,
        "phone" to Icons.Default.Phone,
        "laptop" to Icons.Default.Laptop,
        "photo_camera" to Icons.Default.PhotoCamera,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "local_parking" to Icons.Default.LocalParking,
        "train" to Icons.Default.Train,
        "subway" to Icons.Default.Subway,
        "local_taxi" to Icons.Default.LocalTaxi,
        "star" to Icons.Default.Star,
        "favorite" to Icons.Default.Favorite,
        "attach_money" to Icons.Default.AttachMoney,
        "savings" to Icons.Default.Savings,
        "account_balance" to Icons.Default.AccountBalance,
        "credit_card" to Icons.Default.CreditCard,
        "build" to Icons.Default.Build,
        "child_care" to Icons.Default.ChildCare,
        "wc" to Icons.Default.Wc,
        "local_cafe" to Icons.Default.LocalCafe,
        "local_bar" to Icons.Default.LocalBar,
        "local_grocery_store" to Icons.Default.LocalGroceryStore,
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
        "more_horiz" to Icons.Default.MoreHoriz,
        "local_dining" to Icons.Default.LocalDining,
        "computer" to Icons.Default.Computer,
        "wallet" to Icons.Default.AccountBalanceWallet,
        "car" to Icons.Default.DirectionsCar,
        "bike" to Icons.Default.PedalBike,
        "bus" to Icons.Default.DirectionsBus,
        "subway" to Icons.Default.DirectionsTransit,
        "walk" to Icons.Default.DirectionsWalk,
        "gift" to Icons.Default.CardGiftcard,
        "cake" to Icons.Default.Cake,
        "beach" to Icons.Default.BeachAccess,
        "hotel" to Icons.Default.Hotel,
        "flight_takeoff" to Icons.Default.FlightTakeoff,
        "flight_land" to Icons.Default.FlightLand,
        "map" to Icons.Default.Map,
        "place" to Icons.Default.Place,
        "location" to Icons.Default.LocationOn,
        "cloud" to Icons.Default.Cloud,
        "sunny" to Icons.Default.WbSunny,
        "moon" to Icons.Default.NightsStay,
        "star_border" to Icons.Default.StarBorder,
        "grade" to Icons.Default.Grade,
        "thumb_up" to Icons.Default.ThumbUp,
        "favorite_border" to Icons.Default.FavoriteBorder,
        "heart_broken" to Icons.Default.HeartBroken,
        "emoji_people" to Icons.Default.EmojiPeople,
        "person" to Icons.Default.Person,
        "group" to Icons.Default.Group,
        "people" to Icons.Default.People,
        "family" to Icons.Default.FamilyRestroom,
        "baby" to Icons.Default.ChildCare,
        "pregnant" to Icons.Default.PregnantWoman,
        "elderly" to Icons.Default.Elderly,
        "accessible" to Icons.Default.Accessible,
        "healing" to Icons.Default.Healing,
        "medical" to Icons.Default.LocalHospital,
        "vaccines" to Icons.Default.Vaccines,
        "medication" to Icons.Default.Medication,
        "health" to Icons.Default.HealthAndSafety,
        "sanitizer" to Icons.Default.Sanitizer,
        "masks" to Icons.Default.Masks,
        "soap" to Icons.Default.Soap,
        "clean_hands" to Icons.Default.CleanHands,
        "cleaning" to Icons.Default.CleaningServices,
        "house" to Icons.Default.House,
        "apartment" to Icons.Default.Apartment,
        "business" to Icons.Default.Business,
        "company" to Icons.Default.CorporateFare,
        "meeting" to Icons.Default.MeetingRoom,
        "conference" to Icons.Default.DesktopMac,
        "desk" to Icons.Default.Desk,
        "chair" to Icons.Default.EventSeat,
        "bed" to Icons.Default.Bed,
        "bathtub" to Icons.Default.Bathtub,
        "shower" to Icons.Default.Shower,
        "kitchen" to Icons.Default.Kitchen,
        "tv" to Icons.Default.Tv,
        "radio" to Icons.Default.Radio,
        "speaker" to Icons.Default.Speaker,
        "headphones" to Icons.Default.Headphones,
        "gamepad" to Icons.Default.SportsEsports,
        "toys" to Icons.Default.Toys,
        "sports" to Icons.Default.Sports,
        "sports_baseball" to Icons.Default.SportsBaseball,
        "sports_basketball" to Icons.Default.SportsBasketball,
        "sports_football" to Icons.Default.SportsFootball,
        "sports_golf" to Icons.Default.SportsGolf,
        "sports_handball" to Icons.Default.SportsHandball,
        "sports_hockey" to Icons.Default.SportsHockey,
        "sports_kabaddi" to Icons.Default.SportsKabaddi,
        "sports_mma" to Icons.Default.SportsMma,
        "sports_motorsports" to Icons.Default.SportsMotorsports,
        "sports_rugby" to Icons.Default.SportsRugby,
        "sports_soccer" to Icons.Default.SportsSoccer,
        "sports_tennis" to Icons.Default.SportsTennis,
        "sports_volleyball" to Icons.Default.SportsVolleyball,
        "surfing" to Icons.Default.Surfing,
        "skateboarding" to Icons.Default.Skateboarding,
        "kayaking" to Icons.Default.Kayaking,
        "paragliding" to Icons.Default.Paragliding,
        "scuba_diving" to Icons.Default.ScubaDiving,
        "sailing" to Icons.Default.Sailing,
        "pool" to Icons.Default.Pool,
        "casino" to Icons.Default.Casino,
        "theater" to Icons.Default.TheaterComedy,
        "music" to Icons.Default.MusicNote,
        "movie_filter" to Icons.Default.MovieFilter,
        "video" to Icons.Default.Videocam,
        "camera_alt" to Icons.Default.PhotoCamera,
        "image" to Icons.Default.Image,
        "palette" to Icons.Default.Palette,
        "brush" to Icons.Default.Brush,
        "color_lens" to Icons.Default.ColorLens,
        "format_paint" to Icons.Default.FormatPaint,
        "design" to Icons.Default.DesignServices,
        "architecture" to Icons.Default.Architecture,
        "engineering" to Icons.Default.Engineering,
        "construction" to Icons.Default.Construction,
        "handyman" to Icons.Default.Handyman,
        "plumbing" to Icons.Default.Plumbing,
        "electrical" to Icons.Default.ElectricalServices,
        "carpenter" to Icons.Default.Carpenter,
        "roofing" to Icons.Default.Roofing,
        "foundation" to Icons.Default.Foundation,
        "fence" to Icons.Default.Fence,
        "grass" to Icons.Default.Grass,
        "yard" to Icons.Default.Yard,
        "garden" to Icons.Default.LocalFlorist,
        "nature" to Icons.Default.Nature,
        "park" to Icons.Default.Park,
        "forest" to Icons.Default.Forest,
        "agriculture" to Icons.Default.Agriculture,
        "eco" to Icons.Default.Eco,
        "recycling" to Icons.Default.Recycling,
        "water" to Icons.Default.WaterDrop,
        "fire" to Icons.Default.LocalFireDepartment,
        "flash" to Icons.Default.FlashOn,
        "light" to Icons.Default.Lightbulb,
        "power" to Icons.Default.Power,
        "battery" to Icons.Default.BatteryFull,
        "charging" to Icons.Default.BatteryChargingFull,
        "signal" to Icons.Default.SignalCellularAlt,
        "wifi" to Icons.Default.Wifi,
        "bluetooth" to Icons.Default.Bluetooth,
        "nfc" to Icons.Default.Nfc,
        "usb" to Icons.Default.Usb,
        "vpn" to Icons.Default.VpnKey,
        "lock" to Icons.Default.Lock,
        "security" to Icons.Default.Security,
        "shield" to Icons.Default.Shield,
        "verified" to Icons.Default.Verified,
        "check_circle" to Icons.Default.CheckCircle,
        "cancel" to Icons.Default.Cancel,
        "error" to Icons.Default.Error,
        "warning" to Icons.Default.Warning,
        "info" to Icons.Default.Info,
        "help" to Icons.Default.Help,
        "question" to Icons.Default.HelpOutline,
        "settings" to Icons.Default.Settings,
        "tune" to Icons.Default.Tune,
        "sliders" to Icons.Default.Tune,
        "options" to Icons.Default.MoreVert,
        "menu" to Icons.Default.Menu,
        "list" to Icons.Default.List,
        "grid" to Icons.Default.GridView,
        "view_list" to Icons.Default.ViewList,
        "view_module" to Icons.Default.ViewModule,
        "dashboard" to Icons.Default.Dashboard,
        "analytics" to Icons.Default.Analytics,
        "assessment" to Icons.Default.Assessment,
        "bar_chart" to Icons.Default.BarChart,
        "pie_chart" to Icons.Default.PieChart,
        "show_chart" to Icons.Default.ShowChart,
        "trending_up" to Icons.Default.TrendingUp,
        "trending_down" to Icons.Default.TrendingDown,
        "trending_flat" to Icons.Default.TrendingFlat,
        "schedule" to Icons.Default.Schedule,
        "timer" to Icons.Default.Timer,
        "alarm" to Icons.Default.Alarm,
        "clock" to Icons.Default.AccessTime,
        "history" to Icons.Default.History,
        "update" to Icons.Default.Update,
        "sync" to Icons.Default.Sync,
        "refresh" to Icons.Default.Refresh,
        "autorenew" to Icons.Default.Autorenew,
        "loop" to Icons.Default.Loop,
        "repeat" to Icons.Default.Repeat,
        "shuffle" to Icons.Default.Shuffle,
        "sort" to Icons.Default.Sort,
        "filter" to Icons.Default.Filter,
        "search" to Icons.Default.Search,
        "find" to Icons.Default.FindInPage,
        "zoom_in" to Icons.Default.ZoomIn,
        "zoom_out" to Icons.Default.ZoomOut,
        "fullscreen" to Icons.Default.Fullscreen,
        "exit_fullscreen" to Icons.Default.FullscreenExit,
        "expand" to Icons.Default.ExpandMore,
        "collapse" to Icons.Default.ExpandLess,
        "arrow_up" to Icons.Default.ArrowUpward,
        "arrow_down" to Icons.Default.ArrowDownward,
        "arrow_left" to Icons.Default.ArrowBack,
        "arrow_right" to Icons.Default.ArrowForward,
        "arrow_back" to Icons.Default.ArrowBack,
        "arrow_forward" to Icons.Default.ArrowForward,
        "navigate_before" to Icons.Default.NavigateBefore,
        "navigate_next" to Icons.Default.NavigateNext,
        "first_page" to Icons.Default.FirstPage,
        "last_page" to Icons.Default.LastPage,
        "chevron_left" to Icons.Default.ChevronLeft,
        "chevron_right" to Icons.Default.ChevronRight,
        "double_arrow" to Icons.Default.DoubleArrow,
        "reply" to Icons.Default.Reply,
        "reply_all" to Icons.Default.ReplyAll,
        "forward" to Icons.Default.Forward,
        "send" to Icons.Default.Send,
        "mail" to Icons.Default.Mail,
        "email" to Icons.Default.Email,
        "drafts" to Icons.Default.Drafts,
        "inbox" to Icons.Default.Inbox,
        "outbox" to Icons.Default.Outbox,
        "archive" to Icons.Default.Archive,
        "unarchive" to Icons.Default.Unarchive,
        "delete" to Icons.Default.Delete,
        "restore" to Icons.Default.Restore,
        "restore_from_trash" to Icons.Default.RestoreFromTrash,
        "delete_forever" to Icons.Default.DeleteForever,
        "clear" to Icons.Default.Clear,
        "backspace" to Icons.Default.Backspace,
        "content_cut" to Icons.Default.ContentCut,
        "content_copy" to Icons.Default.ContentCopy,
        "content_paste" to Icons.Default.ContentPaste,
        "select_all" to Icons.Default.SelectAll,
        "edit" to Icons.Default.Edit,
        "create" to Icons.Default.Create,
        "mode_edit" to Icons.Default.ModeEdit,
        "border_color" to Icons.Default.BorderColor,
        "format_color" to Icons.Default.FormatColorFill,
        "text_format" to Icons.Default.TextFormat,
        "title" to Icons.Default.Title,
        "subject" to Icons.Default.Subject,
        "short_text" to Icons.Default.ShortText,
        "long_text" to Icons.Default.Notes,
        "notes" to Icons.Default.Notes,
        "sticky_note" to Icons.Default.StickyNote2,
        "comment" to Icons.Default.Comment,
        "chat" to Icons.Default.Chat,
        "chat_bubble" to Icons.Default.ChatBubble,
        "forum" to Icons.Default.Forum,
        "feedback" to Icons.Default.Feedback,
        "rate_review" to Icons.Default.RateReview,
        "reviews" to Icons.Default.Reviews,
        "recommend" to Icons.Default.Recommend,
        "thumb_down" to Icons.Default.ThumbDown,
        "sentiment" to Icons.Default.SentimentSatisfied,
        "mood" to Icons.Default.Mood,
        "mood_bad" to Icons.Default.MoodBad,
        "sentiment_dissatisfied" to Icons.Default.SentimentDissatisfied,
        "sentiment_neutral" to Icons.Default.SentimentNeutral,
        "sentiment_very_satisfied" to Icons.Default.SentimentVerySatisfied,
        "sentiment_very_dissatisfied" to Icons.Default.SentimentVeryDissatisfied,
        "emoji_emotions" to Icons.Default.EmojiEmotions,
        "emoji_events" to Icons.Default.EmojiEvents,
        "emoji_flags" to Icons.Default.EmojiFlags,
        "emoji_food" to Icons.Default.EmojiFoodBeverage,
        "emoji_nature" to Icons.Default.EmojiNature,
        "emoji_objects" to Icons.Default.EmojiObjects,
        "emoji_people" to Icons.Default.EmojiPeople,
        "emoji_symbols" to Icons.Default.EmojiSymbols,
        "emoji_transportation" to Icons.Default.EmojiTransportation,
        "add" to Icons.Default.Add,
        "add_box" to Icons.Default.AddBox,
        "add_circle" to Icons.Default.AddCircle,
        "add_circle_outline" to Icons.Default.AddCircleOutline,
        "remove" to Icons.Default.Remove,
        "remove_circle" to Icons.Default.RemoveCircle,
        "remove_circle_outline" to Icons.Default.RemoveCircleOutline,
        "close" to Icons.Default.Close,
        "check" to Icons.Default.Check,
        "check_box" to Icons.Default.CheckBox,
        "check_box_outline" to Icons.Default.CheckBoxOutlineBlank,
        "radio_button" to Icons.Default.RadioButtonChecked,
        "radio_button_unchecked" to Icons.Default.RadioButtonUnchecked,
        "toggle_on" to Icons.Default.ToggleOn,
        "toggle_off" to Icons.Default.ToggleOff,
        "star_half" to Icons.Default.StarHalf,
        "star_outline" to Icons.Default.StarOutline,
        "bookmark" to Icons.Default.Bookmark,
        "bookmark_border" to Icons.Default.BookmarkBorder,
        "bookmarks" to Icons.Default.Bookmarks,
        "collections" to Icons.Default.Collections,
        "collections_bookmark" to Icons.Default.CollectionsBookmark,
        "photo_library" to Icons.Default.PhotoLibrary,
        "photo_album" to Icons.Default.PhotoAlbum,
        "burst_mode" to Icons.Default.BurstMode,
        "camera_roll" to Icons.Default.CameraRoll,
        "movie_creation" to Icons.Default.MovieCreation,
        "movie_filter" to Icons.Default.MovieFilter,
        "music_video" to Icons.Default.MusicVideo,
        "subscriptions" to Icons.Default.Subscriptions,
        "video_library" to Icons.Default.VideoLibrary,
        "library_music" to Icons.Default.LibraryMusic,
        "library_books" to Icons.Default.LibraryBooks,
        "library_add" to Icons.Default.LibraryAdd,
        "queue" to Icons.Default.Queue,
        "queue_music" to Icons.Default.QueueMusic,
        "playlist_play" to Icons.Default.PlaylistPlay,
        "playlist_add" to Icons.Default.PlaylistAdd,
        "playlist_add_check" to Icons.Default.PlaylistAddCheck,
        "recent_actors" to Icons.Default.RecentActors,
        "high_quality" to Icons.Default.HighQuality,
        "hd" to Icons.Default.Hd,
        "closed_caption" to Icons.Default.ClosedCaption,
        "subtitles" to Icons.Default.Subtitles,
        "surround_sound" to Icons.Default.SurroundSound,
        "videocam_off" to Icons.Default.VideocamOff,
        "volume_down" to Icons.Default.VolumeDown,
        "volume_mute" to Icons.Default.VolumeMute,
        "volume_off" to Icons.Default.VolumeOff,
        "volume_up" to Icons.Default.VolumeUp,
        "mic" to Icons.Default.Mic,
        "mic_off" to Icons.Default.MicOff,
        "mic_none" to Icons.Default.MicNone,
        "call" to Icons.Default.Call,
        "call_end" to Icons.Default.CallEnd,
        "call_made" to Icons.Default.CallMade,
        "call_merge" to Icons.Default.CallMerge,
        "call_missed" to Icons.Default.CallMissed,
        "call_missed_outgoing" to Icons.Default.CallMissedOutgoing,
        "call_received" to Icons.Default.CallReceived,
        "call_split" to Icons.Default.CallSplit,
        "phone_callback" to Icons.Default.PhoneCallback,
        "phone_disabled" to Icons.Default.PhoneDisabled,
        "phone_enabled" to Icons.Default.PhoneEnabled,
        "phone_forwarded" to Icons.Default.PhoneForwarded,
        "phone_in_talk" to Icons.Default.PhoneInTalk,
        "phone_locked" to Icons.Default.PhoneLocked,
        "phone_missed" to Icons.Default.PhoneMissed,
        "phone_paused" to Icons.Default.PhonePaused,
        "qr_code" to Icons.Default.QrCode,
        "qr_code_scanner" to Icons.Default.QrCodeScanner,
        "rss_feed" to Icons.Default.RssFeed,
        "screen_share" to Icons.Default.ScreenShare,
        "stop_screen_share" to Icons.Default.StopScreenShare,
        "stay_current_landscape" to Icons.Default.StayCurrentLandscape,
        "stay_current_portrait" to Icons.Default.StayCurrentPortrait,
        "stay_primary_landscape" to Icons.Default.StayPrimaryLandscape,
        "stay_primary_portrait" to Icons.Default.StayPrimaryPortrait,
        "swap_calls" to Icons.Default.SwapCalls,
        "textsms" to Icons.Default.Textsms,
        "voicemail" to Icons.Default.Voicemail,
        "vpn_key" to Icons.Default.VpnKey,
        "add_ic_call" to Icons.Default.AddIcCall,
        "alternate_email" to Icons.Default.AlternateEmail,
        "business" to Icons.Default.Business,
        "call_to_action" to Icons.Default.CallToAction,
        "chat_bubble_outline" to Icons.Default.ChatBubbleOutline,
        "comment" to Icons.Default.Comment,
        "contact_mail" to Icons.Default.ContactMail,
        "contact_phone" to Icons.Default.ContactPhone,
        "contacts" to Icons.Default.Contacts,
        "desktop_access_disabled" to Icons.Default.DesktopAccessDisabled,
        "dialer_sip" to Icons.Default.DialerSip,
        "dialpad" to Icons.Default.Dialpad,
        "domain" to Icons.Default.Domain,
        "domain_disabled" to Icons.Default.DomainDisabled,
        "duo" to Icons.Default.Duo,
        "email" to Icons.Default.Email,
        "forum" to Icons.Default.Forum,
        "import_contacts" to Icons.Default.ImportContacts,
        "import_export" to Icons.Default.ImportExport,
        "invert_colors_off" to Icons.Default.InvertColorsOff,
        "list_alt" to Icons.Default.ListAlt,
        "live_help" to Icons.Default.LiveHelp,
        "location_off" to Icons.Default.LocationOff,
        "mail_outline" to Icons.Default.MailOutline,
        "mark_chat_read" to Icons.Default.MarkChatRead,
        "mark_chat_unread" to Icons.Default.MarkChatUnread,
        "mark_email_read" to Icons.Default.MarkEmailRead,
        "mark_email_unread" to Icons.Default.MarkEmailUnread,
        "message" to Icons.Default.Message,
        "mobile_screen_share" to Icons.Default.MobileScreenShare,
        "nat" to Icons.Default.Nat,
        "no_sim" to Icons.Default.NoSim,
        "pause_presentation" to Icons.Default.PausePresentation,
        "person_add_disabled" to Icons.Default.PersonAddDisabled,
        "print_disabled" to Icons.Default.PrintDisabled,
        "qr_code_2" to Icons.Default.QrCode2,
        "read_more" to Icons.Default.ReadMore,
        "ring_volume" to Icons.Default.RingVolume,
        "rtt" to Icons.Default.Rtt,
        "screen_share" to Icons.Default.ScreenShare,
        "sentiment_satisfied_alt" to Icons.Default.SentimentSatisfiedAlt,
        "sip" to Icons.Default.Sip,
        "speaker_phone" to Icons.Default.SpeakerPhone,
        "stay_current_landscape" to Icons.Default.StayCurrentLandscape,
        "stay_current_portrait" to Icons.Default.StayCurrentPortrait,
        "stay_primary_landscape" to Icons.Default.StayPrimaryLandscape,
        "stay_primary_portrait" to Icons.Default.StayPrimaryPortrait,
        "stop_screen_share" to Icons.Default.StopScreenShare,
        "swap_calls" to Icons.Default.SwapCalls,
        "textsms" to Icons.Default.Textsms,
        "unsubscribe" to Icons.Default.Unsubscribe,
        "voicemail" to Icons.Default.Voicemail,
        "vpn_key" to Icons.Default.VpnKey,
        "add_location" to Icons.Default.AddLocation,
        "add_location_alt" to Icons.Default.AddLocationAlt,
        "add_road" to Icons.Default.AddRoad,
        "agriculture" to Icons.Default.Agriculture,
        "alt_route" to Icons.Default.AltRoute,
        "atm" to Icons.Default.Atm,
        "beenhere" to Icons.Default.Beenhere,
        "bike_scooter" to Icons.Default.BikeScooter,
        "breakfast_dining" to Icons.Default.BreakfastDining,
        "brunch_dining" to Icons.Default.BrunchDining,
        "bus_alert" to Icons.Default.BusAlert,
        "car_rental" to Icons.Default.CarRental,
        "car_repair" to Icons.Default.CarRepair,
        "category" to Icons.Default.Category,
        "celebration" to Icons.Default.Celebration,
        "cleaning_services" to Icons.Default.CleaningServices,
        "compass_calibration" to Icons.Default.CompassCalibration,
        "delivery_dining" to Icons.Default.DeliveryDining,
        "departure_board" to Icons.Default.DepartureBoard,
        "design_services" to Icons.Default.DesignServices,
        "dinner_dining" to Icons.Default.DinnerDining,
        "directions" to Icons.Default.Directions,
        "directions_bike" to Icons.Default.DirectionsBike,
        "directions_boat" to Icons.Default.DirectionsBoat,
        "directions_bus" to Icons.Default.DirectionsBus,
        "directions_car" to Icons.Default.DirectionsCar,
        "directions_railway" to Icons.Default.DirectionsRailway,
        "directions_run" to Icons.Default.DirectionsRun,
        "directions_subway" to Icons.Default.DirectionsSubway,
        "directions_transit" to Icons.Default.DirectionsTransit,
        "directions_walk" to Icons.Default.DirectionsWalk,
        "dry_cleaning" to Icons.Default.DryCleaning,
        "edit_attributes" to Icons.Default.EditAttributes,
        "edit_location" to Icons.Default.EditLocation,
        "edit_road" to Icons.Default.EditRoad,
        "electric_bike" to Icons.Default.ElectricBike,
        "electric_car" to Icons.Default.ElectricCar,
        "electric_moped" to Icons.Default.ElectricMoped,
        "electric_rickshaw" to Icons.Default.ElectricRickshaw,
        "electric_scooter" to Icons.Default.ElectricScooter,
        "electrical_services" to Icons.Default.ElectricalServices,
        "ev_station" to Icons.Default.EvStation,
        "fastfood" to Icons.Default.Fastfood,
        "festival" to Icons.Default.Festival,
        "flight" to Icons.Default.Flight,
        "hail" to Icons.Default.Hail,
        "handyman" to Icons.Default.Handyman,
        "hardware" to Icons.Default.Hardware,
        "home_repair_service" to Icons.Default.HomeRepairService,
        "icecream" to Icons.Default.Icecream,
        "layers" to Icons.Default.Layers,
        "layers_clear" to Icons.Default.LayersClear,
        "liquor" to Icons.Default.Liquor,
        "local_activity" to Icons.Default.LocalActivity,
        "local_airport" to Icons.Default.LocalAirport,
        "local_atm" to Icons.Default.LocalAtm,
        "local_bar" to Icons.Default.LocalBar,
        "local_cafe" to Icons.Default.LocalCafe,
        "local_car_wash" to Icons.Default.LocalCarWash,
        "local_convenience_store" to Icons.Default.LocalConvenienceStore,
        "local_dining" to Icons.Default.LocalDining,
        "local_drink" to Icons.Default.LocalDrink,
        "local_fire_department" to Icons.Default.LocalFireDepartment,
        "local_florist" to Icons.Default.LocalFlorist,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "local_grocery_store" to Icons.Default.LocalGroceryStore,
        "local_hospital" to Icons.Default.LocalHospital,
        "local_hotel" to Icons.Default.LocalHotel,
        "local_laundry_service" to Icons.Default.LocalLaundryService,
        "local_library" to Icons.Default.LocalLibrary,
        "local_mall" to Icons.Default.LocalMall,
        "local_movies" to Icons.Default.LocalMovies,
        "local_offer" to Icons.Default.LocalOffer,
        "local_parking" to Icons.Default.LocalParking,
        "local_pharmacy" to Icons.Default.LocalPharmacy,
        "local_phone" to Icons.Default.LocalPhone,
        "local_pizza" to Icons.Default.LocalPizza,
        "local_play" to Icons.Default.LocalPlay,
        "local_police" to Icons.Default.LocalPolice,
        "local_post_office" to Icons.Default.LocalPostOffice,
        "local_printshop" to Icons.Default.LocalPrintshop,
        "local_see" to Icons.Default.LocalSee,
        "local_shipping" to Icons.Default.LocalShipping,
        "local_taxi" to Icons.Default.LocalTaxi,
        "lunch_dining" to Icons.Default.LunchDining,
        "map" to Icons.Default.Map,
        "maps_ugc" to Icons.Default.MapsUgc,
        "medical_services" to Icons.Default.MedicalServices,
        "menu_book" to Icons.Default.MenuBook,
        "miscellaneous_services" to Icons.Default.MiscellaneousServices,
        "money" to Icons.Default.Money,
        "moped" to Icons.Default.Moped,
        "multiple_stop" to Icons.Default.MultipleStop,
        "museum" to Icons.Default.Museum,
        "my_location" to Icons.Default.MyLocation,
        "navigation" to Icons.Default.Navigation,
        "near_me" to Icons.Default.NearMe,
        "near_me_disabled" to Icons.Default.NearMeDisabled,
        "nightlife" to Icons.Default.Nightlife,
        "no_meals" to Icons.Default.NoMeals,
        "no_transfer" to Icons.Default.NoTransfer,
        "not_listed_location" to Icons.Default.NotListedLocation,
        "park" to Icons.Default.Park,
        "pedal_bike" to Icons.Default.PedalBike,
        "person_pin" to Icons.Default.PersonPin,
        "person_pin_circle" to Icons.Default.PersonPinCircle,
        "pest_control" to Icons.Default.PestControl,
        "pest_control_rodent" to Icons.Default.PestControlRodent,
        "pin_drop" to Icons.Default.PinDrop,
        "place" to Icons.Default.Place,
        "plumbing" to Icons.Default.Plumbing,
        "railway_alert" to Icons.Default.RailwayAlert,
        "ramen_dining" to Icons.Default.RamenDining,
        "rate_review" to Icons.Default.RateReview,
        "restaurant" to Icons.Default.Restaurant,
        "restaurant_menu" to Icons.Default.RestaurantMenu,
        "run_circle" to Icons.Default.RunCircle,
        "satellite" to Icons.Default.Satellite,
        "set_meal" to Icons.Default.SetMeal,
        "smoke_free" to Icons.Default.SmokeFree,
        "smoking_rooms" to Icons.Default.SmokingRooms,
        "snowmobile" to Icons.Default.Snowmobile,
        "store_mall_directory" to Icons.Default.StoreMallDirectory,
        "streetview" to Icons.Default.Streetview,
        "subway" to Icons.Default.Subway,
        "takeout_dining" to Icons.Default.TakeoutDining,
        "taxi_alert" to Icons.Default.TaxiAlert,
        "terrain" to Icons.Default.Terrain,
        "theater_comedy" to Icons.Default.TheaterComedy,
        "traffic" to Icons.Default.Traffic,
        "train" to Icons.Default.Train,
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

    val icons = iconMap.values.toList()

    fun getIconByName(name: String): ImageVector? {
        return iconMap[name]
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerDialog(
    currentIcon: String?,
    currentIsEmoji: Boolean,
    onDismiss: () -> Unit,
    onIconSelected: (String, Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "选择图标",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                IconGrid(
                    currentIcon = currentIcon,
                    onIconSelected = { iconName ->
                        onIconSelected(iconName, false)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@Composable
private fun IconGrid(
    currentIcon: String?,
    onIconSelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.height(240.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CategoryIconProvider.iconMap.toList()) { (iconName, icon) ->
            val isSelected = currentIcon == iconName

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) PrimaryGreen.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .then(
                        if (isSelected) Modifier.border(2.dp, PrimaryGreen, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .clickable { onIconSelected(iconName) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryIconDisplay(
    icon: String,
    isEmoji: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 24
) {
    // 只使用图标，不再支持emoji
    val imageVector = CategoryIconProvider.getIconByName(icon) ?: Icons.Default.MoreHoriz
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(size.dp),
        tint = MaterialTheme.colorScheme.onSurface
    )
}

data class CustomCategory(
    val name: String,
    val icon: String,
    val isEmoji: Boolean,
    val color: Color,
    val type: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCategoryDialog(
    existingCategories: List<CustomCategory>,
    onDismiss: () -> Unit,
    onConfirm: (CustomCategory) -> Unit,
    editingCategory: CustomCategory? = null,
    defaultType: Int = Transaction.TYPE_EXPENSE
) {
    var categoryName by remember { mutableStateOf(editingCategory?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(editingCategory?.icon ?: CategoryIconProvider.iconMap.keys.first()) }
    var selectedIsEmoji by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(editingCategory?.color ?: PrimaryGreen) }
    var categoryType by remember { mutableIntStateOf(editingCategory?.type ?: defaultType) }
    var showIconPicker by remember { mutableStateOf(false) }

    val colors = listOf(
        Color(0xFFF97316),
        Color(0xFF8B5CF6),
        Color(0xFF3B82F6),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF6B7280),
        Color(0xFF14B8A6),
        Color(0xFF84CC16)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingCategory == null) "添加分类" else "编辑分类",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryType == Transaction.TYPE_EXPENSE,
                        onClick = { categoryType = Transaction.TYPE_EXPENSE },
                        label = { Text("支出") },
                        leadingIcon = if (categoryType == Transaction.TYPE_EXPENSE) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = categoryType == Transaction.TYPE_INCOME,
                        onClick = { categoryType = Transaction.TYPE_INCOME },
                        label = { Text("收入") },
                        leadingIcon = if (categoryType == Transaction.TYPE_INCOME) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                Text(
                    text = "图标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(selectedColor.copy(alpha = 0.15f))
                            .clickable { showIconPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryIconDisplay(
                            icon = selectedIcon,
                            isEmoji = selectedIsEmoji,
                            size = 32
                        )
                    }
                    TextButton(onClick = { showIconPicker = true }) {
                        Text("更换图标")
                    }
                }

                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                        .padding(2.dp)
                                        .border(2.dp, color, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onConfirm(CustomCategory(
                            name = categoryName,
                            icon = selectedIcon,
                            isEmoji = selectedIsEmoji,
                            color = selectedColor,
                            type = categoryType
                        ))
                    }
                },
                enabled = categoryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = selectedIcon,
            currentIsEmoji = selectedIsEmoji,
            onDismiss = { showIconPicker = false },
            onIconSelected = { icon, isEmoji ->
                selectedIcon = icon
                selectedIsEmoji = isEmoji
                showIconPicker = false
            }
        )
    }
}