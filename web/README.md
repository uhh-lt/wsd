# Web subprojects README

## Important facts 

- This project was bootstrapped with [Create React App](https://github.com/facebookincubator/create-react-app).
- Additionally the Node.js web framework Express is used as a production web server, following the the starter project [react-node-example](https://github.com/alanbsmith/react-node-example/).
- A `Dockerfile` is provided
- As a component library for the React application [material-ui.com](http://www.material-ui.com) is used.
- Redux is used to manage Reacts application state. However I was not happy with this solution, quite a lot of boilerplate code found its way into `src/actions` and `src/reducers`.

## Running during development

```
npm install
export REACT_APP_WSP_API_HOST=http://localhost:9000 # Or wherever the API is deployed to
npm start
```

## Deployment

Please see top level README.

## Diving into React with basic Javascript knowledge

At the point of writing, React promotes JSX instead of plain JS, so you will find JSX code here.
The [reactpatterns.com](http://reactpatterns.com) website might help you getting started fast with surprising syntax you might encounter without knowing JSX.
