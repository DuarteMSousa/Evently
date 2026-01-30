@echo off

kubectl apply -f ./infrastructure --recursive
kubectl apply -f ./ingress

kubectl apply -f ./microservices/cart

kubectl apply -f ./microservices/events

kubectl apply -f ./microservices/fileGeneration

timeout /t 30

kubectl apply -f ./microservices/notifications

kubectl apply -f ./microservices/orders

kubectl apply -f ./microservices/organizations

timeout /t 30

kubectl apply -f ./microservices/payments

kubectl apply -f ./microservices/refunds

kubectl apply -f ./microservices/reviews

timeout /t 30

kubectl apply -f ./microservices/ticketManagement

kubectl apply -f ./microservices/tickets

timeout /t 30

kubectl apply -f ./microservices/users

kubectl apply -f ./microservices/venues


pause
