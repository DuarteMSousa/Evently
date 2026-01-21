@echo off

docker login

docker build -t duarte15sousa/evently_api_gateway APIGateway && docker push duarte15sousa/evently_api_gateway
docker build -t duarte15sousa/evently_cart Cart && docker push duarte15sousa/evently_cart
docker build -t duarte15sousa/evently_eureka_server EurekaServer && docker push duarte15sousa/evently_eureka_server
docker build -t duarte15sousa/evently_events Events && docker push duarte15sousa/evently_events
docker build -t duarte15sousa/evently_file_generation FileGeneration && docker push duarte15sousa/evently_file_generation
docker build -t duarte15sousa/evently_notifications Notifications && docker push duarte15sousa/evently_notifications
docker build -t duarte15sousa/evently_orders Orders && docker push duarte15sousa/evently_orders
docker build -t duarte15sousa/evently_organizations Organizations && docker push duarte15sousa/evently_organizations
docker build -t duarte15sousa/evently_payments Payments && docker push duarte15sousa/evently_payments
docker build -t duarte15sousa/evently_refunds Refunds && docker push duarte15sousa/evently_refunds
docker build -t duarte15sousa/evently_reviews Reviews && docker push duarte15sousa/evently_reviews
docker build -t duarte15sousa/evently_ticket_management TicketManagement && docker push duarte15sousa/evently_ticket_management
docker build -t duarte15sousa/evently_tickets Tickets && docker push duarte15sousa/evently_tickets
docker build -t duarte15sousa/evently_users Users && docker push duarte15sousa/evently_users
docker build -t duarte15sousa/evently_venues Venues && docker push duarte15sousa/evently_venues

echo.
echo Build + Push concluído
pause
