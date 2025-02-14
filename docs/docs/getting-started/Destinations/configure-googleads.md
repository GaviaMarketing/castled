---
sidebar_position: 7
---

# Google Ads

Google ads is an online advertising platform, where advertisers bid to display brief advertisements, service offerings, product listings, or videos to web users

## Creating an app connection

Castled establishes connection to your GoogleAds destination via OAuth. Select GoogleAds as the source type, enter the OAuth ClientID and ClientSecret of the oauth app and also GoogleAds developer token from the GoogleAds console. Login via Google to create an app connection in Castled. 

Follow the steps [here](https://support.google.com/cloud/answer/6158849?hl=en) to setup OAuth on your google cloud console. Follow the link [here](https://developers.google.com/google-ads/api/docs/first-call/dev-token) to obtain your Google Ads developer token.

## Creating a sync pipeline

### Select customer Id

Select the google ads customer id, where you want to sync the data. This will include customer ids, which are directly linked to your google account as well as customer ids you manage.

### Syncing users data for GoogleAds Customer Audience


<iframe width="560" height="315" src="https://www.youtube.com/embed/5miEEnH2Gu4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

You can create a segment of users whom you want to show targeted ads, in your cloud data warehouse and keep them in sync with the Google Ads custom audience list, which can be used in your google ads campaigns.

Select **Customer Match List** as the resource type and then select customer match type. Google ads allows you to sync either the contact info(which contains personal information of a user) or userIds(User ids from external systems) or your mobile device ids to the customer match list. Google Ads will index users who match this criteria and show your campaign ads to them.

Finally select the customer match list, which you have created on the google ads console

![customer match sync config](/img/screens/destinations/gads/gads_customer_match_sync_config.png)


### Syncing click conversions(online or offline)


<iframe width="560" height="315" src="https://www.youtube.com/embed/4DVVSW334fs" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>


For syncing click conversions, select **Click Conversions** as the resource type. Select the name of the click conversion, which you have created on the Google Ads console. Finally select the **Click Conversion Timezone**. Click Conversion Timezone will be used to convert the click conversion time field coming from the source to the actual point in time. If the incoming timestamp column is a timezone aware timezone, this value wont be used.

![click conversions sync config](/img/screens/destinations/gads/gads_customer_match_sync_config.png)


**Google Click Id** and **Conversion time** are mandatory fields while mapping the incoming query fields  to click conversion object fields. Conversion curreny and Conversion value are optional parameters. If not provided, they will be taken from the conversion value and currency provided while creating the conversion object. You can also attach more information associated with the click conversion using custom conversion variables( for eg: conversion region)

![click conversions mapping](/img/screens/destinations/gads/click_conversions_mapping.png)


### Syncing call conversions(online or offline)

For syncing click conversions, select **Call Conversions** as the resource type. Select the name of the call conversion, which you have created on the Google Ads console. Finally select the **Call Conversion Timezone**. Calls Conversion Timezone will be used to convert the call conversion time field coming from the source to the actual point in time. If the incoming timestamp column is a timezone aware timezone, this value wont be used.

![call conversions sync config](/img/screens/destinations/gads/call_conversion_sync_config.png)